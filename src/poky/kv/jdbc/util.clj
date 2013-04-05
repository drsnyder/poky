(ns poky.kv.jdbc.util
  (:require [poky.kv.core :as kv.core]
            [environ.core :refer [env]]
            [clojure.java.jdbc :as sql])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(defn create-db-spec
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
   (create-db-spec dsn "org.postgresql.Driver")))

(defn pool
  [spec]
  (delay 
    (let [cpds (doto (ComboPooledDataSource.)
                 (.setDriverClass (:classname spec)) 
                 (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
                 (.setUser (:user spec))
                 (.setPassword (:password spec))
                 (.setMinPoolSize 3)
                 (.setMaxIdleTimeExcessConnections (* 30 60))
                 (.setMaxIdleTime (* 3 60 60)))] 
      {:datasource cpds})))


(defn jdbc-get
  [conn table key-column k]
  (sql/with-connection 
    conn
    (sql/with-query-results 
      results
      [(format "SELECT %s.* FROM %s WHERE %s = ?" table table key-column) k]
      (first (vec results)))))

(defn jdbc-mget
  [conn table key-column ks]
  (let [query (format "SELECT %s.* FROM %s WHERE %s IN (%s)" table table key-column 
                      (clojure.string/join "," (take (count ks) (cycle ["?"]))))]
    (sql/with-connection 
      conn
      (sql/with-query-results results
                              (vec (flatten [query ks]))
                              (vec results)))))

(defn jdbc-set
  [conn table key-column value-column k value]
  (sql/with-connection 
    conn
    (sql/update-or-insert-values 
      table [(format "%s = ?" key-column) k] 
      {(keyword key-column) k (keyword value-column) value})))


(defn jdbc-delete
  [conn table key-column k]
  (sql/with-connection
    conn
    (sql/delete-rows
      table [(format "%s = ?" key-column) k])))

