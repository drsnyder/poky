(ns poky.kv.jdbc.text
  (:require [poky.kv.core :as kv.core]
            [poky.kv.jdbc.util :as store]))

(def ^:private default-pool-size 3)

(defn- wrap-get
  [conn table key-column value-column k & params]
  (when-let [r (store/jdbc-get conn table key-column k)]
    {(get r (keyword key-column))
     (get r (keyword value-column))}))

(defn- wrap-mget
  [conn table key-column value-column ks & params]
  (when-let [rows (store/jdbc-mget conn table key-column ks)]
    (into {} (for [r rows
                   :let [k (get r (keyword key-column))
                         v (get r (keyword value-column))]
                   :when k]
               [k v]))))

(extend-type poky.kv.core.RDMSKeyValueStore
  kv.core/KeyValueProtocol
  (get* [this k params]
    (wrap-get (kv.core/conn this) (kv.core/table this) (kv.core/key-column this) (kv.core/value-column this) k params))
  (get* [this k]
    (wrap-get (kv.core/conn this) (kv.core/table this) (kv.core/key-column this) (kv.core/value-column this) k))
  (mget* [this ks params]
    (wrap-mget (kv.core/conn this) (kv.core/table this) (kv.core/key-column this) (kv.core/value-column this) ks params))
  (mget* [this ks]
    (wrap-mget (kv.core/conn this) (kv.core/table this) (kv.core/key-column this) (kv.core/value-column this) ks))
  (set* [this k value]
    (store/jdbc-set (kv.core/conn this) 
                    (kv.core/table this) 
                    (kv.core/key-column this) 
                    (kv.core/value-column this) 
                    k 
                    value))
  (delete* [this k]
    (store/jdbc-delete (kv.core/conn this) (kv.core/table this) (kv.core/key-column this) k)))
    
(defn create
  ([dsn table key-column value-column min-pool-size]
   (poky.kv.core.RDMSKeyValueStore.
     (store/pool (store/create-db-spec dsn) :min-pool-size min-pool-size) 
     table 
     key-column 
     value-column))
  ([dsn]
   (create dsn "poky_text" "key" "data" default-pool-size)))
