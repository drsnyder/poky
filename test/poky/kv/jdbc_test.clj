(ns poky.kv.jdbc-test
  (:require [poky.kv.core :as kv]
            [poky.kv.jdbc :as kv.jdbc]
            [poky.kv.jdbc.util :as jdbc-util]
            [poky.util :as util]
            [environ.core :refer [env]]
            [clojure.java.jdbc :as sql]
            [midje.sweet :refer :all])
  (:import [poky.kv.core.KeyValue]
           [poky.kv.core.Connection]))
(def bucket (str (.name *ns*)))
(def S (atom nil))

(facts :compare-seq-first
       (kv.jdbc/compare-seq-first '(0) 1) => falsey
       (kv.jdbc/compare-seq-first '(1) 1) => truthy
       (kv.jdbc/compare-seq-first nil 1) => falsey)


(facts :jdbc :get
       ; midje doesn't allow you to test metaconstants for equality. inserting
       ; bogus values here
       (kv/get* (kv.jdbc/create ..store..) ..bucket.. ..key..) => {"some-key" "some-value" :modified_at "modified"}
       (provided
         (kv.jdbc/create-connection ..store..) => (delay ..store..)
         (kv.jdbc/jdbc-get ..store.. ..bucket.. ..key..) => {:key "some-key" :data "some-value" :modified_at "modified"})
       (kv/get* (kv.jdbc/create ..store..) ..bucket.. ..key..) => nil
       (provided
         (kv.jdbc/create-connection ..store..) => (delay ..store..)
         (kv.jdbc/jdbc-get ..store.. ..bucket.. ..key..) => nil))


(facts :jdbc :set
       (kv/set* (kv.jdbc/create ..store..) ..bucket.. ..key.. ..value..) => :updated
       (provided
         (kv.jdbc/create-connection ..store..) => (delay ..store..)
         (kv.jdbc/jdbc-set ..store.. ..bucket.. ..key.. ..value..) => '(1))
       (kv/set* (kv.jdbc/create ..store..) ..bucket.. ..key.. ..value..) => :rejected
       (provided
         (kv.jdbc/create-connection ..store..) => (delay ..store..)
         (kv.jdbc/jdbc-set ..store.. ..bucket.. ..key.. ..value..) => '(0)))

(facts :jdbc :delete
       (kv/delete* (kv.jdbc/create ..store..) ..bucket.. ..key..) => true
       (provided
         (kv.jdbc/create-connection ..store..) => (delay ..store..)
         (kv.jdbc/jdbc-delete ..store.. ..bucket.. ..key..) => '(1))
       (kv/delete* (kv.jdbc/create ..store..) ..bucket.. ..key..) => false
       (provided
         (kv.jdbc/create-connection ..store..) => (delay ..store..)
         (kv.jdbc/jdbc-delete ..store.. ..bucket.. ..key..) => '(0)))

(defn clear-testing-bucket [state bucket]
  (kv.jdbc/purge-bucket (kv/connection state) bucket))


(with-state-changes [(around :facts (do (reset! S (kv.jdbc/create (env :database-url)))
                                        ; alternatively, use sql/transaction
                                        ; and rollback. that's possible, but
                                        ; would require some refactoring of
                                        ; kv.jdbc to support in nested within
                                        ; sql/connection.
                                        (clear-testing-bucket @S bucket)
                                        ?form
                                        (kv/close @S)))]
  (facts :integration :jdbc-set
         (kv.jdbc/jdbc-set (kv/connection @S) bucket "key" "value") => (contains {:key "key" :data "value"})
         (kv.jdbc/jdbc-set (kv/connection @S) bucket "key" "value") => '(1))

  (facts :integration :jdbc-get
         (kv.jdbc/jdbc-get (kv/connection @S) bucket "key") => falsey
         (kv.jdbc/jdbc-set (kv/connection @S) bucket "key" "value") => (contains {:key "key" :data "value"})
         (kv.jdbc/jdbc-get (kv/connection @S) bucket "key") => (contains {:key "key" :data "value"}))

  (facts :integration :jdbc-delete
         (kv.jdbc/jdbc-set (kv/connection @S) bucket "key" "value") => (contains {:key "key" :data "value"})
         (kv.jdbc/jdbc-delete (kv/connection @S) bucket "key") => '(1)
         (kv.jdbc/jdbc-delete (kv/connection @S) bucket "key") => '(0)))



(with-state-changes [(around :facts (do (reset! S (kv.jdbc/create (env :database-url)))
                                        ; alternatively, use sql/transaction
                                        ; and rollback. that's possible, but
                                        ; would require some refactoring of
                                        ; kv.jdbc to support in nested within
                                        ; sql/connection.
                                        (clear-testing-bucket @S bucket)
                                        ?form
                                        (kv/close @S)))]
  (facts :integration :set
         (kv/set* @S bucket "key" "value") => :inserted
         (kv/set* @S bucket "key" "value") => :updated)

  (facts :integration :get
         (kv/get* @S bucket "key") => falsey
         (kv/set* @S bucket "key" "value") => :inserted
         (kv/get* @S bucket "key") => (contains {"key" "value"}))

  (facts :integration :delete
         (kv/set* @S bucket "key" "value") => :inserted
         (kv/delete* @S bucket "key") => truthy
         (kv/delete* @S bucket "key") => falsey))
