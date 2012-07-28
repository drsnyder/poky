(ns poky.core
  (:use [poky.vars :only [*table*]]
        [clojure.java.jdbc :as sql :only [with-connection]])
  (:require [poky.db :as db]))


(defn add [conn k v]
  (sql/with-connection conn 
                (sql/update-or-insert-values poky.vars/*table* ["key = ?" k]  {:key k :value v})))


(defn mget [conn ks]
    (db/query conn 
              (format "SELECT key, value FROM %s WHERE key IN (%s)" 
                      poky.vars/*table* 
                      (clojure.string/join "," (map #(sql/as-quoted-str "'" %) ks)))))
  
