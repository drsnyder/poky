(ns poky.kv.jdbc.util
  (:require (poky [util :as util])
            [clojure.java.jdbc :as sql]
            [clojure.string :as string]
            [clojure.tools.logging :refer [warn infof]]
            [environ.core :refer [env]])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource
     [java.sql SQLException Timestamp]))

(def ^:private default-min-pool-size 3)
(def ^:private default-max-pool-size 15)
(def ^:private default-driver "org.postgresql.Driver")
(def ^:private poky-column-types
  {:bucket "text"
   :key "text"
   :data "text"
   :created_at "timestamptz"
   :modified_at "timestamptz"})

(defn- wrap-join [b d a coll] (str b (string/join d coll) a))

(defn pg-array [coll] (wrap-join "ARRAY[" \, \] coll))

(defn create-db-spec
  "Given a dsn and optionally a driver create a db spec that can be used with pool to
  create a connection pool."
  ([dsn driver]
   (let [uri (java.net.URI. dsn)
         host (.getHost uri)
         scheme (.getScheme uri)
         [user pass] (clojure.string/split (.getUserInfo uri) #":")
         port (.getPort uri)
         port (if (= port -1) 5432 port)
         path (.substring (.getPath uri) 1)]
     {:classname driver
      :subprotocol scheme
      :subname (str "//" host ":" port "/" path)
      :user user
      :password pass}))
  ([dsn]
   (create-db-spec dsn default-driver)))

(defn pool
  "Create a connection pool."
  [spec &{:keys [min-pool-size max-pool-size]
          :or {min-pool-size default-min-pool-size max-pool-size default-max-pool-size}}]
  (infof "Creating pool with min %d and max %d connections." min-pool-size max-pool-size)
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname spec))
               (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
               (.setUser (:user spec))
               (.setPassword (:password spec))
               (.setMinPoolSize min-pool-size)
               (.setMaxPoolSize max-pool-size)
               (.setMaxIdleTimeExcessConnections (* 30 60))
               (.setMaxIdleTime (* 3 60 60)))]
      {:datasource cpds}))

(defn create-connection
  "Create a connection and delay it."
  [dsn]
  (delay (pool (create-db-spec dsn) :max-pool-size (util/parse-int (env :max-pool-size default-max-pool-size)))))

(defn close-connection
  "Close the connection of a JdbcKeyValue object."
  [connection-object]
  (.close (:datasource connection-object)))

(defn purge-bucket
  "Should only be used in testing."
  [conn b]
  (sql/with-connection conn
    (sql/delete-rows "poky"
       ["bucket=?" b])))

(defn format-sql-exception
  "Formats the contents of an SQLException and return string.
  Similar to clojure.java.jdbc/print-sql-exception, but doesn't write to *out*"
  [^SQLException exception]
  (let [^Class exception-class (class exception)]
    (format (str "%s:" \newline
                 " Message: %s" \newline
                 " SQLState: %s" \newline
                 " Error Code: %d")
            (.getSimpleName exception-class)
            (.getMessage exception)
            (.getSQLState exception)
            (.getErrorCode exception))))

(defn warn-sql-exception
  "Outputs a formatted SQLException to log warn"
  [^SQLException e]
  (doall (map #(warn (format-sql-exception %))
              (iterator-seq (.iterator e)))))

(defmacro with-logged-connection [conn & body]
    `(try
         (sql/with-connection ~conn
              ~@body)
         (catch SQLException e#
             (warn-sql-exception e#))))


(defn jdbc-get
  "Get the tuple at bucket b and key k. Returns a map with the attributes of the table."
  [conn b k]
  (with-logged-connection conn
    (sql/with-query-results
      results
      ["SELECT * FROM poky WHERE bucket=? AND key=?" b k]
      (first results))))

(defn jdbc-set
  "Set a bucket b and key k to value v. Returns a map with key :result upon success.
  The value at result will be one of \"inserted\", \"updated\" or \"rejected\"."
  ([conn b k v modified]
   (with-logged-connection conn
     (sql/with-query-results results ["SELECT upsert_kv_data(?, ?, ?, ?) AS result" b k v modified]
       (first results))))
  ([conn b k v]
   (with-logged-connection conn
     (sql/with-query-results results ["SELECT upsert_kv_data(?, ?, ?) AS result" b k v]
       (first results)))))

(defn jdbc-delete
  "Delete the value at bucket b and key k. Returns true on success and false if the
  tuple does not exist."
  [conn b k]
  (with-logged-connection conn
    (sql/delete-rows "poky"
       ["bucket=? AND key=?" b k])))

;; ======== MULTI

(defn- mget-sproc-conds
  "Returns"
  [conds]
  (loop [[row & conds] conds
         sql (list)
         params (list)]
    (if row
      (let [qs (->> (map poky-column-types (keys row))
                    (map #(str "[?::text, ?::" % "]"))
                    (wrap-join \[ \, \]))]
        (recur conds
               (conj sql qs)
               (concat params (mapcat (juxt (comp name key) val) row))))
      [sql params])))

(defn- mget-sproc
  "Returns the SQL params vector (ie [sql & params]) for the stored procedure,
  mget(text, text[], text[][][])

  Example:
  (mget-sproc 'b' [{:key 'key1'} {:key 'key2'}) =>
  ['SELECT * FROM mget(?, ARRAY[?, ?], ARRAY[[[?, ?]], [[?, ?]]])' 'b' 'key1' 'key2' 'key' 'key1' 'key' 'key2']"
  [bucket conds]
  (let [[cond-sql cond-params] (mget-sproc-conds conds)
        sql (wrap-join "SELECT * FROM mget(" \, \)
                       ["?::text"
                        (pg-array (repeat (count conds) "?::text"))
                        (pg-array cond-sql)])]
    (apply vector sql bucket
           (concat (map :key conds)
                   cond-params))))

(defn jdbc-mget
  [conn bucket conds]
  (when-let [q (mget-sproc bucket conds)]
    (with-logged-connection conn
      (sql/with-query-results results q
        (doall results)))))

(defn- mset-prepared-statement
  "Returns a PreparedStatement for mset. Assumes open SQL connection.
  Disclaimer: I don't normally manually create prepared statements, but I created
  this function while debugging It ends up being more efficient then normal
  constructing & passing of arguments as 2nd arg to sql/with-query-results."
  [data]
  (let [query (str "SELECT upsert_kv_data(b, k, v, t) FROM (VALUES "
                   (string/join "," (repeat (count data) "(?,?,?,?::timestamptz)"))
                   ") AS data (b, k, v, t)")
        stmt (sql/prepare-statement (sql/connection) query)]
    (dorun
      (map-indexed
        (fn [ix {b :bucket k :key v :data t :modified_at}]
          (let [offset (* ix 4)]
            (doto stmt
              (.setObject (+ offset 1) b)
              (.setObject (+ offset 2) k)
              (.setObject (+ offset 3) v)
              (.setObject (+ offset 4) t))))
        data))
    stmt))

(defn jdbc-mset
  "Upserts multiple records in data. Records are hashmaps with following fields:
  :bucket      (required)
  :key         (required)
  :data        (required)
  :modified_at (optional)
  "
  [conn data]
  (with-logged-connection conn
    (sql/with-query-results results
      [(mset-prepared-statement data)]
      (doall results))))
