(ns poky.kv.core)

(defprotocol RDMSKeyValueStoreProtocol
  (conn [this])
  (table [this])
  (key-column [this])
  (value-column [this]))


(defprotocol KeyValueProtocol
  (get* [this k params] [this k])
  (mget* [this ks params] [this ks])
  (set* [this k value])
  (delete* [this k]))

(defrecord RDMSKeyValueStore [conn table key-column value-column]
  RDMSKeyValueStoreProtocol
  (conn [this]
    (:conn this))
  (table [this]
    (:table this))
  (key-column [this]
    (:key-column this))
  (value-column [this]
    (:value-column this)))

(defn create
  [conn table key-column value-column]
  (RDMSKeyValueStore. conn table key-column value-column))
