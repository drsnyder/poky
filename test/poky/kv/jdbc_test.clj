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


; this is a little less than ideal, but it works. to use with-state-changes,
; you have to nest the facts like so as opposed to the wrapping convention
; above. i believe it is the let that is causing the issue but I haven't tested
; it
(with-state-changes [(before :facts (do (reset! S (kv.jdbc/create (env :database-url)))
                                 (clear-testing-bucket @S bucket)))
                     (after :facts (do (kv/close @S)))]
  (let [k (util/random-string 10)
        v (util/random-string 10)]
    (fact :integration
          (kv.jdbc/jdbc-set (kv/connection @S) bucket k v) => (contains {:key k :data v}))
    (fact :integration
          (do
            (kv.jdbc/jdbc-set (kv/connection @S) bucket k v)
            (kv.jdbc/jdbc-set (kv/connection @S) bucket k v)) => '(1))))
