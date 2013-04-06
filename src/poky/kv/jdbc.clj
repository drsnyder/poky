(ns poky.kv.jdbc
  (:require [poky.kv.core :as kv.core]
            [poky.kv.jdbc.util :as store]))

(defrecord JDBCKeyValueStore [conn table key-column value-column]
  kv.core/KeyValueProtocol
  (get* [this k]
    (store/jdbc-get @conn table key-column k))
  (mget* [this ks]
    (when-let [rows (store/jdbc-mget @conn table key-column ks)]
      (into {} (map vec 
                    (for [r rows] (list
                               (get r (keyword key-column))
                               (get r (keyword value-column))))))))
  (set* [this k value]
    (store/jdbc-set @conn table key-column value-column k value))
  (delete* [this k]
    (store/jdbc-mget @conn table key-column k)))
    
(defn create
  ([dsn table key-column value-column]
   (JDBCKeyValueStore. 
     (store/pool (store/create-db-spec dsn)) 
     table 
     key-column 
     value-column))
  ([dsn]
   (create dsn "poky" "key" "value")))
