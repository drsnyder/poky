(ns poky.kv.core)

(defprotocol KeyValue
  (get* [this b k] [this b k params])
  (mget* [this b ks] [this b ks params])
  (set* [this b k value])
  (delete* [this b k]))
