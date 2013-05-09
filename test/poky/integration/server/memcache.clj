; TODO: this needs to be brought up-to-date and migrated to midje
; let's use :integration as a tag instead of a directory
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


(defn pass-through-memcache-handler-test [in expected]
  (let [c (channel)
        r (memcache-handler c {} in)
        s (channel-seq c)]
    (fact s => expected)))


(pass-through-memcache-handler-test ["SET" "abc" "0" "3" "123"] (list ["STORED"]))
(pass-through-memcache-handler-test ["SET" "def" "0" "3" "456"] (list ["STORED"]))
(pass-through-memcache-handler-test ["get" "abc"] (list ["VALUE" "abc" "0" "123"] ["END"]))
(pass-through-memcache-handler-test ["get" "abc def"] (list ["VALUE" "abc" "0" "123"] ["VALUE" "def" "0" "456"] ["END"]))
(pass-through-memcache-handler-test ["delete" "abc"] (list ["DELETED"]))

