(ns poky.kv.core)

(defprotocol KeyValueProtocol
  (get* [this k])
  (mget* [this ks])
  (set* [this k value])
  (delete* [this k]))

