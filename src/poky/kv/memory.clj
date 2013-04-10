(ns poky.kv.memory
  (:require [poky.kv.core :as kv.core]))

(defrecord MemoryKeyValueStore [^:clojure.lang.Atom data]
  kv.core/KeyValueProtocol
  (get* [this k params]
    (get @data k))
  (get* [this k]
    (get @data k))
  (mget* [this ks params]
    (select-keys @data ks))
  (mget* [this ks]
    (select-keys @data ks))
  (set* [this k value]
    (swap! data assoc k value))
  (delete* [this k]
    (swap! data dissoc k)))
    
(defn create
  ([data]
   (MemoryKeyValueStore. (atom data)))
  ([]
   (create {})))
