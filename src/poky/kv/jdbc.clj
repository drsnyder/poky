(ns poky.kv.jdbc
  (:require [poky.util :as util]
            [poky.kv.core :refer :all]
            [poky.kv.jdbc.util :refer :all]
            [clojure.tools.logging :refer [warnf]]
            [clojure.java.jdbc :as sql]
            [clojure.string :as string])
  (:import [poky.kv.core.KeyValue]
           [poky.kv.core.KeyValueMulti]
           [poky.kv.core.Connection]))

(declare get-connection)

(defrecord JdbcKeyValue [conn]
  KeyValue
  (get* [this b k params]
    (get* this b k))
  (get* [this b k]
    (when-let [row (jdbc-get @conn b k)]
      {(:key row) (:data row) :modified_at (:modified_at row)}))
  (set* [this b k value]
    (set* this b k value {}))
  (set* [this b k value params]
    (when-let [ret (jdbc-set @conn b k value (get params :modified nil))]
      (keyword (:result ret))))
  (delete* [this b k]
    (util/first= (jdbc-delete @conn b k) 1))

  KeyValueMulti
  (mget* [this b conds]
    (into {} (map (juxt :key :data) (jdbc-mget @conn b conds))))
  (mset* [this b data]
    ;; do sequentually. TODO set in single query
    (doall (map #(when-let [k (:key %)] (set* this b k %)) data)))
  (mdelete* [this b conds]
    ;; do sequentually. TODO set in single query
    (doall (map #(when-let [k (:key %)] (delete* this b k)) conds)))

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
