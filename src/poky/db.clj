(ns poky.db
  (:use [clojure.java.jdbc :as sql :only [with-connection]]
        [clojure.pprint])
  (:require [poky.vars :as pvars])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource)
  (:import [java.lang.reflect Method]))


; subname 127.0.0.1:3306/poky
; user 
; password
(defn pool
  [subname user password]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass "org.postgresql.Driver") 
               (.setJdbcUrl (str "jdbc:postgresql://" subname))
               (.setUser user)
               (.setPassword password)
               ;; expire excess connections after 30 minutes of inactivity:
               (.setMaxIdleTimeExcessConnections (* 30 60))
               ;; expire connections after 3 hours of inactivity:
               (.setMaxIdleTime (* 3 60 60)))] 
    {:datasource cpds}))


(def pooled-db (delay (pool pvars/*subname* pvars/*user* pvars/*password*)))

(defn connection [] @pooled-db)

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
