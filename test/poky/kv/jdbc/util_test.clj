(ns poky.kv.jdbc.util-test
  (:require [poky.kv.jdbc.util :refer :all]
            [poky.kv.core :as kv]
            [environ.core :refer [env]]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clojure.java.jdbc :as sql]
            [midje.sweet :refer :all]))

(def bucket (str (.name *ns*)))
(def S (atom nil))

(with-state-changes [(around :facts (do (reset! S (create-connection (env :database-url)))
                                        ; alternatively, use sql/transaction
                                        ; and rollback. that's possible, but
                                        ; would require some refactoring of
                                        ; kv.jdbc to support in nested within
                                        ; sql/connection.
                                        (purge-bucket @@S bucket)
                                        ?form
                                        (close-connection @@S)))]
  (facts :integration :jdbc-set
         (jdbc-set @@S bucket "key" "value") => (contains {:key "key" :data "value"})
         (jdbc-set @@S bucket "key" "value") => '(1)
         ; TODO: currently broken because we get a unique key violation with
         ; update-or-insert-values
         (jdbc-set @@S bucket "key" "value" (tc/to-sql-date (t/minus (t/now) (t/days 1)))) => '(0))

  (facts :integration :jdbc-get
         (jdbc-get @@S bucket "key") => falsey
         (jdbc-set @@S bucket "key" "value") => (contains {:key "key" :data "value"})
         (jdbc-get @@S bucket "key") => (contains {:key "key" :data "value"}))

  (facts :integration :jdbc-delete
         (jdbc-set @@S bucket "key" "value") => (contains {:key "key" :data "value"})
         (jdbc-delete @@S bucket "key") => '(1)
         (jdbc-delete @@S bucket "key") => '(0)))
