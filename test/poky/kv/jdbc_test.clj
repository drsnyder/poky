(ns poky.kv.jdbc-test
  (:require [poky.kv.core :as kv]
            [poky.kv.jdbc :as kv.jdbc]
            [poky.kv.jdbc.util :refer :all]
            [poky.util :as util]
            [environ.core :refer [env]]
            [clojure.java.jdbc :as sql]
            [midje.sweet :refer :all])
  (:import [poky.kv.core.KeyValue]
           [poky.kv.core.Connection]))

(def bucket (str (.name *ns*)))
(def S (atom nil))



(facts :jdbc :get
       ; midje doesn't allow you to test metaconstants for equality. inserting
       ; bogus values here
       (kv/get* (kv.jdbc/create ..store..) ..bucket.. ..key..) => {"some-key" "some-value" :modified_at "modified"}
       (provided
         (create-connection ..store..) => (delay ..store..)
         (jdbc-get ..store.. ..bucket.. ..key..) => {:key "some-key" :data "some-value" :modified_at "modified"})

       (kv/get* (kv.jdbc/create ..store..) ..bucket.. ..key..) => nil
       (provided
         (create-connection ..store..) => (delay ..store..)
         (jdbc-get ..store.. ..bucket.. ..key..) => nil))


(facts :jdbc :set
       (kv/set* (kv.jdbc/create ..store..) ..bucket.. ..key.. ..value..) => :updated
       (provided
         (create-connection ..store..) => (delay ..store..)
         (jdbc-set ..store.. ..bucket.. ..key.. ..value..) => '(1))

       (kv/set* (kv.jdbc/create ..store..) ..bucket.. ..key.. ..value..) => :rejected
       (provided
         (create-connection ..store..) => (delay ..store..)
         (jdbc-set ..store.. ..bucket.. ..key.. ..value..) => '(0)))

(facts :jdbc :delete
       (kv/delete* (kv.jdbc/create ..store..) ..bucket.. ..key..) => true
       (provided
         (create-connection ..store..) => (delay ..store..)
         (jdbc-delete ..store.. ..bucket.. ..key..) => '(1))

       (kv/delete* (kv.jdbc/create ..store..) ..bucket.. ..key..) => false
       (provided
         (create-connection ..store..) => (delay ..store..)
         (jdbc-delete ..store.. ..bucket.. ..key..) => '(0)))



(with-state-changes [(around :facts (do (reset! S (kv.jdbc/create (env :database-url)))
                                        (purge-bucket (kv/connection @S) bucket)
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
