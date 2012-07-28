(ns poky.db
  (:use [clojure.java.jdbc :as sql :only [with-connection]]
        [clojure.pprint])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource)
  (:import [java.lang.reflect Method]))

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
