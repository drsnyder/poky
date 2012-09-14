(ns poky.integration.core
  (:use [clojure.test]
        [midje.sweet])
  (:require [poky.core :as poky]))


(fact
  (let [r (poky/add "abc" "123")]
    (or (:insert r) (:update r))) => truthy)

(let [a (poky/add "abc" "123")
      r (poky/gets ["abc"])]
  (fact (:values r) => truthy)
  (fact (:key (first (:values r))) => "abc")
  (fact (:value (first (:values r))) => "123"))

(let [r (poky/gets ["hhonhrgabsaboneuhsodou"])]
      (fact (:values r) => []))

(let [r (poky/delete "abc")]
  (fact (:deleted r) => 1))

