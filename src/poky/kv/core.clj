(ns poky.kv.core)

(defprotocol KeyValue
  "A key value protocol."
  (get* [this b k] [this b k params]
        "Get the object at the specified bucket and key. Returns a map with
        {k value
        :modified_at ts
        :version version}")
  (mget* [this b ks] [this b ks params]
        "Deprecated.")
  (set* [this b k value] [this b k value params]
        "Set the object at the specified bucket and key with value. Returns:
        :created
        :updated
        :rejected
        if the object was created or altered, false otherwise.")
  (delete* [this b k]
        "Delete the object at the specified bucket and key. Returns true if the object
        was deleted and false otherwise."))

(defprotocol Connection
  (connection [this] "Get the connection.")
  (close [this] "Close the connection"))
