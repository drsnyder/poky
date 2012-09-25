(ns poky.db
  (:use [clojure.java.jdbc :as sql :only [with-connection]]
        [clojure.pprint])
  (:require [poky.vars :as pvars])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource)
  (:import [java.lang.reflect Method]))


; jdbc-url jdbc:postgresql://127.0.0.1:5432/poky
; user 
; password
(defn pool
  ([config]
   (let [cpds (doto (ComboPooledDataSource.)
                (.setDriverClass (:driver config)) 
                (.setJdbcUrl (:jdbc-url config))
                (.setUser (:user config))
                (.setPassword (:password config))
                (.setMinPoolSize 3)
                ;; expire excess connections after 30 minutes of inactivity:
                (.setMaxIdleTimeExcessConnections (* 30 60))
                ;; expire connections after 3 hours of inactivity:
                (.setMaxIdleTime (* 3 60 60)))] 
     {:datasource cpds}))
  ([jdbc-url user password]
   (pool jdbc-url user password "org.postgresql.Driver")))


(defn connect! []
  (pvars/set-connection! 
    (delay (pool (pvars/get-config)))))

(defn connection [] 
  (deref (pvars/get-connection)))

(defmacro with-conn
  [& body]
  `(sql/with-connection (connection)
     ~@body))


(defn query
  [#^String query & params]
  (let [p [{:fetch-size 1000} query]]
    (with-conn (connection)
               (sql/with-query-results results
                                       (vec (if params
                                         (flatten (conj p params))
                                         p))
                                       (vec results)))))

(defn insert-or-update 
  [k v]
  (sql/with-connection 
    (connection)
    (sql/update-or-insert-values 
      poky.vars/*table* ["key = ?" k]  {:key k :value v})))

(defn delete
  [k]
  (sql/with-connection
    (connection)
    (sql/delete-rows
      poky.vars/*table* ["key = ?" k])))
