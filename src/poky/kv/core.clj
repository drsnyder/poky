(ns poky.kv.core)

(defprotocol KeyValueProtocol
  (get* [this b k params]
        [this b k])
  (mget* [this b ks params]
         [this b ks])
  (set* [this b k value])
  (delete* [this b k]))
