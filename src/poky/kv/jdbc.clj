(ns poky.kv.jdbc
  (:require [poky.kv.core :refer :all]
            [poky.kv.jdbc.util :refer [create-db-spec pool]]
            [clojure.java.jdbc :as sql]
            [clojure.string :as string] ))

(defn jdbc-get
  [conn b k]
  (sql/with-connection conn
    (sql/with-query-results
      results
      ["SELECT * FROM poky WHERE bucket=? AND key=?" b k]
      (first results))))

(defn jdbc-mget
  [conn b ks]
  (sql/with-connection conn
    (sql/with-query-results results
      (vec (concat [(format "SELECT * FROM poky WHERE bucket=? AND key IN (%s)"
                            (string/join "," (repeat (count ks) "?")))
                    b]
                   ks))
      (doall results))))

(defn jdbc-set
  [conn b k v]
  (sql/with-connection conn
    (sql/update-or-insert-values "poky"
      ["bucket=? AND key=?" b k]
      {:bucket b :key k :data v})))

(defn jdbc-delete
  [conn b k]
  (sql/with-connection conn
    (sql/delete-rows "poky"
      ["bucket=? AND key=?" b k])))

(defrecord JdbcKeyValue [conn]
  KeyValue
  (get* [this b k params]
    (get* this b k))
  (get* [this b k]
    (when-let [row (jdbc-get conn b k)]
      {(:key row) (:data row)}))
  (mget* [this b ks params]
    (mget* this b ks))
  (mget* [this b ks]
    (into {} (map (juxt :key :data) (jdbc-mget conn b ks))))
  (set* [this b k value]
    (jdbc-set conn b k value))
  (delete* [this b k]
    (jdbc-delete conn b k)))

(defn create
  [dsn]
  (->JdbcKeyValue (pool (create-db-spec dsn))))
