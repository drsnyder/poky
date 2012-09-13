(ns poky.integration.server.memcache
  (:use [poky.server.memcache]
        [clojure.test]
        [midje.sweet])
  (:require [lamina.core.utils :as lamina]))


(def server-set-test ["SET" "abc" "0" "0" "3" "123"])
(def server-get-test ["get" "abc def"])
(def server-delete-test ["delete" "abc"])

(fact 
  (let [r (storage->dispatch :set server-set-test)]
    (or (:insert r) (:update r)) => truthy))

(fact 
  (let [r (storage->dispatch :get server-get-test)]
    (:values r) => truthy))

(fact 
  (let [r (storage->dispatch :gets server-get-test)]
    (:values r) => truthy))

(fact 
  (let [r (storage->dispatch :delete server-delete-test)]
    (:deleted r) => truthy))


(fact
  (memcache-handler [] {} server-set-test) => ["STORED"])

(fact
  (memcache-handler [] {} server-get-test) => ["END"])

(fact 
  (memcache-handler [] {} server-delete-test) => ["DELETED"])

