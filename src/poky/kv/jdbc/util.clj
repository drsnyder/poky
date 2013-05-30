(ns poky.kv.jdbc.util
  (:require [clojure.java.jdbc :as sql]
            [clojure.string :as string]
            [environ.core :refer [env]])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(def ^:private default-min-pool-size 3)
(def ^:private default-max-pool-size 3)
(def ^:private default-driver "org.postgresql.Driver")

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
  (delay (pool (create-db-spec dsn) :max-pool-size (env :max-pool-size default-max-pool-size))))

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

(defn jdbc-get
  "Get the tuple at bucket b and key k. Returns a map with the attributes of the table."
  [conn b k]
  (sql/with-connection conn
    (sql/with-query-results
      results
      ["SELECT * FROM poky WHERE bucket=? AND key=?" b k]
      (first results))))

(defn jdbc-mget
  "Deprecated."
  [conn b ks]
  (sql/with-connection conn
    (sql/with-query-results results
      (vec (concat [(format "SELECT * FROM poky WHERE bucket=? AND key IN (%s)"
                            (string/join "," (repeat (count ks) "?")))
                    b]
                   ks))
      (doall results))))

(defn jdbc-set
  "Set a bucket b and key k to value v. Returns true on success and false on failure."
  ([conn b k v]
  (sql/with-connection conn
    (sql/update-or-insert-values "poky"
       ["bucket=? AND key=?" b k]
       {:bucket b :key k :data v})))
  ([conn b k v modified]
   (sql/with-connection conn
     (sql/update-or-insert-values "poky"
       ["bucket=? AND key=?" b k]
       {:bucket b :key k :data v :modified_at modified}))))


(defn jdbc-delete
  "Delete the value at bucket b and key k. Returns true on success and false if the
  tuple does not exist."
  [conn b k]
  (sql/with-connection conn
    (sql/delete-rows "poky"
       ["bucket=? AND key=?" b k])))
