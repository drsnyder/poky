(ns poky.kv.memory
  (:require [poky.kv.core :refer :all]))

(defrecord MemoryKeyValueStore [^:clojure.lang.Atom data]
  KeyValue
  (get* [this b k]
    (get-in @data [b k]))
  (get* [this b k params]
    (get* this b k))
  (mget* [this b ks]
    (select-keys @data ks))
  (mget* [this b ks params]
    (select-keys (@data b) ks))
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
