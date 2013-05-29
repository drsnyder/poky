(ns poky.kv.jdbc
  (:require [poky.util :as util]
            [poky.kv.core :refer :all]
            [poky.kv.jdbc.util :refer :all]
            [clojure.tools.logging :refer [warnf]]
            [clojure.java.jdbc :as sql]
            [clojure.string :as string])
  (:import [poky.kv.core.KeyValue]
           [poky.kv.core.Connection]))

(declare get-connection)

(defrecord JdbcKeyValue [conn]
  KeyValue
  (get* [this b k params]
    (get* this b k))
  (get* [this b k]
    (when-let [row (jdbc-get @conn b k)]
      {(:key row) (:data row) :modified_at (:modified_at row)}))
  (mget* [this b ks params]
    (mget* this b ks))
  (mget* [this b ks]
    (into {} (map (juxt :key :data) (jdbc-mget @conn b ks))))
  (set* [this b k value]
    (when-let [ret (jdbc-set @conn b k value)]
      (cond
        (and (seq? ret) (util/first= ret 1)) :updated
        (and (seq? ret) (util/first= ret 0)) :rejected
        (map? ret) :inserted
        :else false)))
  (set* [this b k value params]
    (set* this b k value))
  (delete* [this b k]
    (util/first= (jdbc-delete @conn b k) 1))

  Connection
  (connection [this]
    (get-connection this))
  (close [this]
    (close-connection (connection this))))

(defn get-connection
  "Get the connection from a JdbcKeyValue object."
  [jdbc-keyvalue-object]
  @(:conn jdbc-keyvalue-object))

(defn create
  [dsn]
  (->JdbcKeyValue (create-connection dsn)))
