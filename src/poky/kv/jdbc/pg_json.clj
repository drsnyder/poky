(ns poky.kv.jdbc.pg-json
  (:require [poky.kv.core :as kv.core]
            [poky.kv.jdbc.util :as store]
            [cheshire.core :as json]
            [clojure.java.jdbc :as sql])
  (:import [org.postgresql.util PGobject]))

(defprotocol PGObjectJSON
  (get-json [this])
  (set-json [this value]))

(extend-type org.postgresql.util.PGobject
  PGObjectJSON
  (get-json [this]
    (when (= (.getType this) "json")
      (json/decode (.getValue this))))
  (set-json [this value]
    (doto this
      (.setType "json")
      (.setValue (json/encode value)))))

(declare jdbc-insert-or-update-json)

(extend-type poky.kv.core.RDMSKeyValueStore
  kv.core/KeyValueProtocol
  (get* [this k params]
    (when-let [r (store/jdbc-get (kv.core/conn this) (kv.core/table this) (kv.core/key-column this) k)]
      (get-json (:data r))))
  (get* [this k]
    (prn "pg_json get")
    (when-let [r (store/jdbc-get (kv.core/conn this) (kv.core/table this) (kv.core/key-column this) k)]
      (get-json (:data r))))
  (mget* [this ks params]
    (when-let [rows (store/jdbc-mget (kv.core/conn this) (kv.core/table this) (kv.core/key-column this) ks)]
      (into {} (map vec 
                    (for [r rows] (list
                               (get r (keyword (kv.core/key-column this))
                               (get r (keyword (kv.core/value-column this))))))))))
  (mget* [this ks]
    (when-let [rows (store/jdbc-mget (kv.core/conn this) (kv.core/table this) (kv.core/key-column this) ks)]
      (into {} (map vec 
                    (for [r rows] (list
                               (get r (keyword (kv.core/key-column this))
                               (get r (keyword (kv.core/value-column this))))))))))
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
   (create dsn "poky_json" "key" "data")))


;(defrecord PGJSON [^:kv.core/RDMSKeyValueStore store]
;  kv.core/KeyValueProtocol
;  (get* [this k params]
;    (when-let [r (kvstore/get* (.store this) k)]
;      (get-json (:data r))))
;  (get* [this k]
;    (kv.core/get* this k nil))
;  (mget* [this ks params]
;    (into {} (map vec 
;                  (for [[k v] (kv.core/mget* (.store this) ks params)]
;                    (list k
;                          (get-json v))))))
;  (mget* [this ks]
;    (kv.core/mget* this ks nil))
;  (set* [this k value]
;    (let [o (set-json (org.postgresql.util.PGobject.) value)]
;    (kv.core/set* (.store this) k o)))
;  (delete* [this k]
;    (kv.core/delete* (.store this) k)))
;    
;(defn create
;  [^:kvstore/JDBCKeyValueStore store] 
;  (PGJSON. store))

(defn jdbc-insert-or-update-json
  [conn])

