(ns poky.core
  (:use [poky.vars :only [*table*]]
        [clojure.java.jdbc :as sql :only [with-connection]])
  (:require [poky.db :as db]))


; is there a multi-key/value pair version of update or insert?
(defn add [conn k v]
  (try 
    (let [r (sql/with-connection 
              conn 
              (sql/update-or-insert-values poky.vars/*table* ["key = ?" k]  {:key k :value v}))]
      (cond
        (map? r) {:insert true}
        (seq? r) {:update true}
        :else {:error "unknown"}))
    (catch Exception e {:error (str "Exception: " (.getMessage e))})))


(defn gets [conn ks]
  (try
    {:values 
     (db/query conn 
              (format "SELECT key, value FROM %s WHERE key IN (%s)" 
                      poky.vars/*table* 
                      (clojure.string/join "," (map #(sql/as-quoted-str "'" %) ks))))}
    (catch Exception e {:error (str "Exception: " (.getMessage e))})))

  
