(ns poky.kv.jdbc-test
  (:require [poky.kv.core :as kv]
            [poky.kv.jdbc :as kv.jdbc]
            [poky.kv.jdbc.util :refer :all]
            [poky.util :as util]
            [environ.core :refer [env]]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clojure.java.jdbc :as sql]
            [midje.sweet :refer :all])
  (:import [poky.kv.core.KeyValue]
           [poky.kv.core.Connection]
           [java.sql.Timestamp]))

(def bucket (str (.name *ns*)))
(def S (atom nil))


(facts :jdbc :get
       ; midje doesn't allow you to test metaconstants for equality. inserting
       ; bogus values here
       (kv/get* (kv.jdbc/create ..store..) ..bucket.. ..key..) => {"some-key" "some-value"
                                                                   :modified_at "modified"}
       (provided
         (create-connection ..store..) => (delay ..store..)
         (jdbc-get ..store.. ..bucket.. ..key..) => {:key "some-key"
                                                     :data "some-value"
                                                     :modified_at "modified"})

       (kv/get* (kv.jdbc/create ..store..) ..bucket.. ..key..) => nil
       (provided
         (create-connection ..store..) => (delay ..store..)
         (jdbc-get ..store.. ..bucket.. ..key..) => nil))


(facts :jdbc :set
       (kv/set* (kv.jdbc/create ..store..) ..bucket.. ..key.. ..value..) => :inserted
       (provided
         (create-connection ..store..) => (delay ..store..)
         (jdbc-set ..store.. ..bucket.. ..key.. ..value.. nil) => {:result "inserted"})

       (kv/set* (kv.jdbc/create ..store..) ..bucket.. ..key.. ..value..) => :updated
       (provided
         (create-connection ..store..) => (delay ..store..)
         (jdbc-set ..store.. ..bucket.. ..key.. ..value.. nil) => {:result "updated"})

       (kv/set* (kv.jdbc/create ..store..) ..bucket.. ..key.. ..value..) => :rejected
       (provided
         (create-connection ..store..) => (delay ..store..)
         (jdbc-set ..store.. ..bucket.. ..key.. ..value.. nil) => {:result "rejected"}))

(facts :jdbc :delete
       (kv/delete* (kv.jdbc/create ..store..) ..bucket.. ..key..) => true
       (provided
         (create-connection ..store..) => (delay ..store..)
         (jdbc-delete ..store.. ..bucket.. ..key..) => '(1))

       (kv/delete* (kv.jdbc/create ..store..) ..bucket.. ..key..) => false
       (provided
         (create-connection ..store..) => (delay ..store..)
         (jdbc-delete ..store.. ..bucket.. ..key..) => '(0)))

(facts :jdbc :mget
  (kv/mget* (kv.jdbc/create ..store..) ..bucket.. ..conds..) => (contains {..key1.. ..data1..
                                                                           ..key2.. ..data2..})
  (provided
    (create-connection ..store..) => (delay ..store..)
    (jdbc-mget ..store.. ..bucket.. ..conds..) => [{:key ..key1..
                                                    :data ..data1..}
                                                   {:key ..key2..
                                                    :data ..data2..}]))

(facts :jdbc :mset
  (let [data [{:key ..key1.. :data ..data1..}]
        every-has-bucket? (partial every? #(= (:bucket %) ..bucket..))]
    (kv/mset* (kv.jdbc/create ..store..) ..bucket.. ..data..) => ...response..
   (provided
     (create-connection ..store..) => (delay ..store..)
     (jdbc-mset ..store.. (as-checker every-has-bucket?)) => ...response..)))



(with-state-changes [(around :facts (do (reset! S (kv.jdbc/create (env :database-url)))
                                        (purge-bucket (kv/connection @S) bucket)
                                        ?form
                                        (kv/close @S)))]
  (facts :integration :set
         (kv/set* @S bucket "key" "value") => :inserted

         (kv/set* @S bucket "key" "value") => :updated
         (kv/set* @S bucket "key" "value" {:modified_at
                                           (tc/to-sql-date
                                             (t/minus (t/now) (t/days 1)))}) => :rejected)

  (facts :integration :get
         (kv/get* @S bucket "key") => falsey
         (kv/set* @S bucket "key" "value") => :inserted
         (kv/get* @S bucket "key") => (contains {"key" "value" :modified_at
                                                 #(instance? java.sql.Timestamp %)}))

  (facts :integration :delete
         (kv/set* @S bucket "key" "value") => :inserted
         (kv/delete* @S bucket "key") => truthy
         (kv/delete* @S bucket "key") => falsey))
