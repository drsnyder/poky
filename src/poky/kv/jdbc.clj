(ns poky.kv.jdbc
  (:require [poky.kv.core :refer :all]
            [poky.kv.jdbc.util :refer [create-db-spec pool]]
            [clojure.java.jdbc :as sql]
            [clojure.string :as string])
  (:import [poky.kv.core.KeyValue]
           [poky.kv.core.Connection]))


(defn purge-bucket
  "Should only be used in testing."
  [conn b]
  (sql/with-connection conn
    (sql/delete-rows "poky"
       ["bucket=?" b])))

(defn compare-seq-first
  "Compare the first value in s to v using =. Complements set and delete.
  The clojure.java.jdbc methods they use return a tuple where the first element is the
  number of records updated. This helper can be used to test that element for the number
  expected."
  [s v]
  (when (seq? s)
    (= (first s) v)))

(defn jdbc-get
  "Get the tuple at bucket b and key k. Returns a map with the attributes of the table."
  [conn b k]
  (sql/with-connection conn
    (sql/with-query-results
      results
      ["SELECT * FROM poky WHERE bucket=? AND key=?" b k]
      (first results))))

(defn jdbc-mget
  "Deprecated."
  [conn b ks]
  (sql/with-connection conn
    (sql/with-query-results results
      (vec (concat [(format "SELECT * FROM poky WHERE bucket=? AND key IN (%s)"
                            (string/join "," (repeat (count ks) "?")))
                    b]
                   ks))
      (doall results))))

(defn jdbc-set
  "Set a bucket b and key k to value v. Returns true on success and false on failure."
  [conn b k v]
  (sql/with-connection conn
    (sql/update-or-insert-values "poky"
       ["bucket=? AND key=?" b k]
       {:bucket b :key k :data v})))


(defn jdbc-delete
  "Delete the value at bucket b and key k. Returns true on success and false if the
  tuple does not exist."
  [conn b k]
  (sql/with-connection conn
    (sql/delete-rows "poky"
       ["bucket=? AND key=?" b k])))

(declare get-connection close-connection)

(defrecord JdbcKeyValue [conn]
  KeyValue
  Connection
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
        (and (seq? ret) (compare-seq-first ret 1)) :updated
        (and (seq? ret) (compare-seq-first ret 0)) :rejected
        (map? ret) :inserted
        :else false)))
  (set* [this b k value params]
    (set* this b k value))
  (delete* [this b k]
    (compare-seq-first (jdbc-delete @conn b k) 1))

  (connection [this]
    (get-connection this))
  (close [this]
    (close-connection this)))

(defn create-connection
  [dsn]
  (delay (pool (create-db-spec dsn))))

(defn close-connection
  "Close the connection of a JdbcKeyValue object."
  [jdbc-keyvalue-object]
  (.close (:datasource @(:conn jdbc-keyvalue-object))))

(defn get-connection
  "Get the connection from a JdbcKeyValue object."
  [jdbc-keyvalue-object]
  @(:conn jdbc-keyvalue-object))

(defn create
  [dsn]
  (->JdbcKeyValue (create-connection dsn)))
