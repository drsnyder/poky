(ns poky.test.server.memcache
  (:use [poky.server.memcache]
        [clojure.test]
        [midje.sweet])
  (:require [lamina.core.utils :as lamina]))

(defprotocol Queueable
             (enqueue [ch v]))

(extend-type clojure.lang.PersistentVector
             Queueable
             lamina/IEnqueue
             (enqueue [ch v] v))

(def server-set-test ["SET" "abc" "0" "0" "3" "123"])
(def server-get-test ["get" "abc def"])
(def server-delete-test ["delete" "abc"])

(fact 
  (cmd-set-key server-set-test) => "abc")

(fact 
  (cmd-set-value server-set-test) => "123")

(fact 
  (first (cmd-gets-keys server-get-test)) => "abc")

(fact 
  (cmd-set-key server-set-test) => "abc")

(fact 
  (cmd-set-value server-set-test) => "123")

(fact
  (first (cmd-gets-keys server-get-test)) => "abc")

(fact
  (second (cmd-gets-keys server-get-test)) => "def")

(fact 
  (cmd-delete-key server-delete-test) => "abc")

; these are working as push-through unit tests via the protocol definition above.
; in practice the return value won't look like this as IEnqueue.enqueue should 
; return true if the message was enqueued to the channel

(fact 
  (cmd->dispatch "set" [] {} server-set-test 
                 (fn [cmd p] {:update true})) => ["STORED"])

(fact 
  (cmd->dispatch "set" [] {} server-set-test 
                 (fn [cmd p] {:insert true})) => ["STORED"])
(fact 
  (cmd->dispatch "set" [] {} server-set-test 
                 (fn [cmd p] {:error "an error"})) => ["SERVER_ERROR" "an error"])
(fact 
  (cmd->dispatch "set" [] {} server-set-test 
                 (fn [cmd p] {})) => ["SERVER_ERROR" "oops, something bad happened while setting."])
(fact 
  (cmd->dispatch "set" [] {} server-set-test 
                 (fn [cmd p] {:error "something bad"})) => ["SERVER_ERROR" "something bad"])


(fact 
  (cmd->dispatch "get" [] {} server-get-test 
                 (fn [cmd p] {:values [{:key "abc" :value "123"} 
                                   {:key "def" :value "456"}]})) => 
  ; fix enqueue above to accumulate
  ;["VALUE" "abc" "0" "3" "123" "VALUE" "def" "0" "3" "456" "END"]
  ["END"])

(fact 
  (cmd->dispatch "get" [] {} server-get-test 
                 (fn [cmd p] {:error "something bad"})) => ["SERVER_ERROR" "something bad"])
(fact 
  (cmd->dispatch "get" [] {} server-get-test 
                 (fn [cmd p] {})) => ["SERVER_ERROR" "oops, something bad happened while getting."])

(fact 
  (cmd->dispatch "delete" [] {} server-delete-test 
                 (fn [cmd p] {:deleted 1})) => ["DELETED"])
(fact 
  (cmd->dispatch "delete" [] {} server-delete-test 
                 (fn [cmd p] {:error "something bad"})) => ["SERVER_ERROR" "something bad"])
(fact 
  (cmd->dispatch "delete" [] {} server-delete-test 
                 (fn [cmd p] {})) => ["SERVER_ERROR" "oops, something bad happened while deleting."])





;; --- integration ---
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

