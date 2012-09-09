(ns poky.test.core
  (:use [clojure.test]
        [midje.sweet])
  (:require [poky.core :as poky]))


(deftest ^:integration test-add
         (let [r (poky/add "abc" "123")]
           (is (or (:insert r) (:update r)))))

(deftest ^:integration test-gets
         (let [a (poky/add "abc" "123")
               r (poky/gets ["abc"])]
           (is (:values r))
           (is (= (:key (first (:values r))) "abc"))
           (is (= (:value (first (:values r))) "123"))))

(deftest ^:integration test-delete
         (let [r (poky/delete "abc")]
           (is (:deleted r))))
