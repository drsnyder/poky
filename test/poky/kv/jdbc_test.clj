(ns poky.kv.jdbc-test
  (:require [poky.kv.core :as kv]
            [poky.kv.jdbc :as store]
            [poky.kv.jdbc.util :as jdbc-util]
            [environ.core :refer [env]]
            [midje.sweet :refer :all])
  (:import [poky.kv.core.KeyValue]))

(def bucket (.name *ns*))

(facts :compare-seq-first
       (store/compare-seq-first '(0) 1) => falsey
       (store/compare-seq-first '(1) 1) => truthy
       (store/compare-seq-first nil 1) => falsey)


(facts :jdbc :get
       ; midje doesn't allow you to test metaconstants for equality. inserting
       ; bogus values here
       (kv/get* (store/create ..store..) ..bucket.. ..key..) => {"some-key" "some-value" :modified_at "modified"}
       (provided
         (store/create-connection ..store..) => (delay ..store..)
         (store/jdbc-get ..store.. ..bucket.. ..key..) => {:key "some-key" :data "some-value" :modified_at "modified"})
       (kv/get* (store/create ..store..) ..bucket.. ..key..) => nil
       (provided
         (store/create-connection ..store..) => (delay ..store..)
         (store/jdbc-get ..store.. ..bucket.. ..key..) => nil))

(facts :jdbc :set
       (kv/set* (store/create ..store..) ..bucket.. ..key.. ..value..) => true
       (provided
         (store/create-connection ..store..) => (delay ..store..)
         (store/jdbc-set ..store.. ..bucket.. ..key.. ..value..) => '(1))
       (kv/set* (store/create ..store..) ..bucket.. ..key.. ..value..) => false
       (provided
         (store/create-connection ..store..) => (delay ..store..)
         (store/jdbc-set ..store.. ..bucket.. ..key.. ..value..) => '(0)))

(facts :jdbc :delete
       (kv/delete* (store/create ..store..) ..bucket.. ..key..) => true
       (provided
         (store/create-connection ..store..) => (delay ..store..)
         (store/jdbc-delete ..store.. ..bucket.. ..key..) => '(1))
       (kv/delete* (store/create ..store..) ..bucket.. ..key..) => false
       (provided
         (store/create-connection ..store..) => (delay ..store..)
         (store/jdbc-delete ..store.. ..bucket.. ..key..) => '(0)))


