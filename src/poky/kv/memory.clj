(ns poky.kv.memory
  (:require [poky.kv.core :as kv.core]))

(defrecord MemoryKeyValueStore [^:clojure.lang.Atom data]
  kv.core/KeyValueProtocol
  (get* [this b k params]
    (get-in @data [b k]))
  (get* [this b k]
    (get-in @data [b k]))
  (mget* [this b ks params]
    (select-keys (@data b) ks))
  (mget* [this b ks]
    (select-keys @data ks))
  (set* [this b k value]
    (swap! data assoc-in [b k] value))
  (delete* [this b k]
    (swap! data #(if-let [bucket (get % b)]
                   (let [bucket' (dissoc bucket k)]
                     (if (seq bucket')
                       (assoc % b bucket')
                       (dissoc % b)))
                   %))))

(defn create
  ([data]
   (MemoryKeyValueStore. (atom data)))
  ([]
   (create {})))
