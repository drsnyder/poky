(ns poky.core
  (:use [poky.vars :only [*table*]]
        [clojure.java.jdbc :as sql :only [with-connection]])
  (:require [poky.db :as db]))


; is there a multi-key/value pair version of update or insert?
(defn add [k v]
  (try 
    (let [r (db/insert-or-update k v)]
      (cond
        (map? r) {:insert true}
        (seq? r) {:update true}
        :else {:error "unknown"}))
    (catch Exception e {:error (str "Exception: " (.getMessage e))})))


(defn gets [ks]
  (try
    {:values 
     (db/query 
              (format "SELECT key, value, octet_length(value) as length FROM %s WHERE key IN (%s)" 
                      poky.vars/*table* 
                      (clojure.string/join "," (map #(sql/as-quoted-str "'" %) ks))))}
    (catch Exception e {:error (str "Exception: " (.getMessage e))})))

  
; TODO: what are the semantics of delete in memcache? what is the return value
; if the object is deleted?
(defn delete [k]
  (try
    {:deleted (first (db/delete k))}
    (catch Exception e {:error (str "Exception " (.getMessage e))})))
