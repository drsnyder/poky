(ns poky.kv.jdbc.util-test
  (:require [poky.kv.jdbc.util :refer :all]
            [poky.kv.core :as kv]
            [poky.util :as util]
            [environ.core :refer [env]]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clojure.java.jdbc :as sql]
            [midje.sweet :refer :all]))

(def bucket (util/sanitize-bucket-name (str (.name *ns*))))
(def S (atom nil))

(with-state-changes [(around :facts (do
                                      (reset! S (create-connection (env :database-url)))
                                      ; alternatively, use sql/transaction
                                      ; and rollback. that's possible, but
                                      ; would require some refactoring of
                                      ; kv.jdbc to support in nested within
                                      ; sql/connection.
                                      (purge-bucket @@S bucket)
                                      ?form
                                      (close-connection @@S)))]
  (facts :integration :jdbc-set
    (jdbc-set @@S bucket "key" "value") => (contains {:result "inserted"})
    (jdbc-set @@S bucket "key" "value") => (contains {:result "updated"})
    (jdbc-set @@S bucket "key" "value"
              (tc/to-sql-date (t/plus (t/now) (t/days 1)))) => (contains {:result "updated"})

    ; unconditionally accept a set without a modified
    (jdbc-set @@S bucket "key" "value") => (contains {:result "updated"})

    ; the modified_at should be 1 day in the future. this should be
    ; rejected
    (jdbc-set @@S bucket "key" "value"
              (tc/to-sql-date (t/now))) => (contains {:result "rejected"}))

  (facts :integration :jdbc-get
    (jdbc-get @@S bucket "key") => falsey
    (jdbc-set @@S bucket "key" "value") => (contains {:result "inserted"})
    (jdbc-get @@S bucket "key") => (contains {:key "key" :data "value"}))

  (facts :integration :jdbc-delete
    (jdbc-set @@S bucket "key" "value") => (contains {:result "inserted"})
    (jdbc-delete @@S bucket "key") => '(1)
    (jdbc-delete @@S bucket "key") => '(0))


  (with-state-changes [(around :facts (do
                                        (jdbc-set @@S bucket "key1" "value1" (tc/to-sql-date (t/now)))
                                        (jdbc-set @@S bucket "key2" "value2" (tc/to-sql-date (t/minus (t/now) (t/minutes 5))))
                                        ?form
                                        (jdbc-delete @@S bucket "key1")
                                        (jdbc-delete @@S bucket "key2")))]
    (facts :integration :jdbc-mget
      ;basic mget
      (jdbc-mget @@S bucket [{:key "key1"} {:key "key2"}])
      => (just
           (contains {:key "key1"})
           (contains {:key "key2"})
           :in-any-order)

      ;empty results
      (jdbc-mget @@S bucket []) => empty?
      (jdbc-mget @@S bucket [{}]) => empty?
      (jdbc-mget @@S bucket [{:key "unknown-key"}]) => empty?

      ;modified_at
      (let [key1-ts (-> (jdbc-get @@S bucket "key1") :modified_at)
            key1-ts-invalid (java.sql.Timestamp. (+ (.getTime key1-ts) 36000))
            key2-ts (-> (jdbc-get @@S bucket "key2") :modified_at)]
        ;valid
        (jdbc-mget @@S bucket [{:key "key1" :modified_at key1-ts}]) => (just (contains {:key "key1"}))
        ;invalid
        (jdbc-mget @@S bucket [{:key "key1" :modified_at key1-ts-invalid}]) => empty?
        ;invalid + valid
        (jdbc-mget @@S bucket [{:key "key1" :modified_at key1-ts-invalid}
                               {:key "key2" :modified_at key2-ts}]) => (just (contains {:key "key2"}))))

    (facts :integration :jdbc-mset
      (jdbc-mset @@S [{:bucket bucket :key "key1" :data "data1"}]) => (contains {:upsert_kv_data "updated"})

      (let [key1-ts (util/http-date->Timestamp "Sat, 29 Jun 2013 22:43:43 GMT")]
        (jdbc-mset @@S [{:bucket bucket :key "key1" :data "data1" :modified_at key1-ts}
                        {:bucket bucket :key "key2" :data "data2"}]) => (list {:upsert_kv_data "rejected"} {:upsert_kv_data "updated"})
        (jdbc-get @@S bucket "key1") => (contains {:bucket bucket :key "key1" :data "data1"})
        (jdbc-get @@S bucket "key2") => (contains {:bucket bucket :key "key2" :data "data2"})))))
