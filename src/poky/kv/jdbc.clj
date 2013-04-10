(ns poky.kv.jdbc
  (:require [poky.kv.core :as kv.core]
            [poky.kv.jdbc.util :as store]))

; FIXME: deprecate this. it's not necessary
(extend-type poky.kv.core.RDMSKeyValueStore
  kv.core/KeyValueProtocol
  (get* [this k params]
    (store/jdbc-get (kv.core/conn this) (kv.core/table this) (kv.core/key-column this) k))
  (get* [this k]
    (store/jdbc-get (kv.core/conn this) (kv.core/table this) (kv.core/key-column this) k))
  (mget* [this ks params]
    (when-let [rows (store/jdbc-mget (kv.core/conn this) (kv.core/table this) (kv.core/key-column this) ks)]
      (into {} (map vec 
                    (for [r rows] (list
                               (get r (keyword (kv.core/key-column this))
                               (get r (keyword (kv.core/value-column this))))))))))
  (mget* [this ks]
    (kv.core/mget* this ks nil))
  (set* [this k value]
    (store/jdbc-set (kv.core/conn this) 
                    (kv.core/table this) 
                    (kv.core/key-column this) 
                    (kv.core/value-column this) 
                    k 
                    value))
  (delete* [this k]
    (store/jdbc-mget (kv.core/conn this) (kv.core/table this) (kv.core/key-column this) k)))
    
(defn create
  ([dsn table key-column value-column]
   (poky.kv.core.RDMSKeyValueStore.
     @(store/pool (store/create-db-spec dsn)) 
     table 
     key-column 
     value-column))
  ([dsn]
   (create dsn "poky_text" "key" "data")))
