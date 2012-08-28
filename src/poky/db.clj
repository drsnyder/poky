(ns poky.db
  (:use [clojure.java.jdbc :as sql :only [with-connection]]
        [clojure.pprint])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource)
  (:import [java.lang.reflect Method]))

;(def db-spec 
;  {:classname "com.postgresql.jdbc.Driver"
;   :subprotocol "postgresql"
;   :subname "//127.0.0.1:3306/poky"
;   :user "myaccount"
;   :password "secret"})

(defn pool
  [spec]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname spec)) 
               (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
               (.setUser (:user spec))
               (.setPassword (:password spec))
               ;; expire excess connections after 30 minutes of inactivity:
               (.setMaxIdleTimeExcessConnections (* 30 60))
               ;; expire connections after 3 hours of inactivity:
               (.setMaxIdleTime (* 3 60 60)))] 
    {:datasource cpds}))


(def pooled-db (delay (pool db-spec)))

(defn db-connection [] @pooled-db)

(defmacro with-conn
  [addr & body]
  `(sql/with-connection ~addr
     ~@body))


(defn query
  [addr #^String query & params]
  (let [p [{:fetch-size 1000} query]]
    (with-conn addr 
               (sql/with-query-results results
                                       (vec (if params
                                         (flatten (conj p params))
                                         p))
                                       (vec results)))))
