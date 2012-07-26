(ns poky.repl-helper
    (:use [clojure.java.jdbc :as sql :only [with-connection]]
                  [clojure.pprint])
    (:import com.mchange.v2.c3p0.ComboPooledDataSource)
    (:import [java.lang.reflect Method]))

; create table poky ( key varchar(1024) not null, value text, constraint thekey primary key (key) );
; export DATABASE_URL=postgresql://drsnyder@localhost:5432/somedb 
(def conn (get (System/getenv) "DATABASE_URL")) 

(defmacro with-conn
  [addr & body]
  `(sql/with-connection (get-connection ~addr)
     ~@body))


(defn query
  [addr #^String query &[params]]
  (let [p [{:fetch-size 1000} query]]
    (with-conn addr 
               (sql/with-query-results results
                                       (if params
                                         (flatten (conj p params))
                                         p)
                                       (vec results)))))

(defn query-map
  "Executes f on each result. Discards result of f."
  [addr #^Method cb #^String query &[params]]
  (let [p [{:fetch-size 1000} query]]
    (with-conn addr 
      (sql/with-query-results results
                              (if params
                                (conj p params)
                                p)
                              (dorun 
                                (map cb results))))))
