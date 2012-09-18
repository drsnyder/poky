(ns poky.integration.server.memcache
  (:use [poky.server.memcache]
        [clojure.test]
        [midje.sweet]
        [lamina.core]))


(def server-delete-test ["delete" "abc"])

(defn find-key [t k]
  (loop [rows (:values t)]
    (if-let [r (first rows)]
      (if (= (:key r) k)
        r
        (recur (rest rows))))))

(defn has-key? [r k]
  (contains? (set (map #(:key %) (:values r))) k))

(fact 
  (let [r (storage->dispatch :set ["SET" "abc" "0" "3" "123"])]
    (or (:insert r) (:update r)) => truthy))

(let [s1 (storage->dispatch :set ["SET" "abc" "0" "3" "123"])
      s2 (storage->dispatch :set ["SET" "def" "0" "3" "456"])
      r (storage->dispatch :get ["get" "abc def"])]
  (fact (:values r) => truthy)
  (fact (find-key r "abc") => truthy)
  (fact (:value (find-key r "abc")) => "123"))

(fact 
  (let [r (storage->dispatch :delete server-delete-test)]
    (:deleted r) => truthy))

(let [s1 (storage->dispatch :set ["SET" "abc" "0" "3" "123"])
      d (storage->dispatch :delete ["delete" "abc"])
      r (storage->dispatch :get ["get" "abc"])]
  (fact (:values r) => [])
  (fact (find-key r "abc") => falsey))


(fact
  (memcache-handler [] {} ["SET" "abc" "0" "3" "123"]) => ["STORED"])

(let [s1 (memcache-handler [] {} ["SET" "abc" "0" "3" "123"])
      s2 (memcache-handler [] {} ["SET" "def" "0" "3" "456"])
      r (memcache-handler [] {} ["get" "abc def"])
      s (set r)]
  ;["VALUE" "abc" "0" "123" "VALUE" "def" "0" "456" "END"])
  (fact
    (first r) => "VALUE")
  (fact 
    (contains? s "abc") => true)
  (fact 
    (contains? s "123") => true)
  (fact
    (nth r 4) => "VALUE")
  (fact 
    (contains? s "def") => true)
  (fact 
    (contains? s "456") => true)
  (fact
    (nth r 8) => "END"))


(let [s1 (memcache-handler [] {} ["SET" "abc" "0" "3" "123"])
      r (memcache-handler [] {} ["get" "abc"])]
  (fact r => ["VALUE" "abc" "0" "123" "END"]))


(fact 
  (memcache-handler [] {} server-delete-test) => ["DELETED"])

