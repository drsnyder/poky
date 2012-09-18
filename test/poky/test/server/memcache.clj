(ns poky.test.server.memcache
  (:use [poky.server.memcache]
        [clojure.test]
        [midje.sweet]
        [lamina.core]))



(def server-set-test ["SET" "abc" "0" "0" "123"])
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

; FIXME: create a wrapper function that does the (let ..), creates the channel
; calls the function, consumes the channel, and tests the results

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

(let [c (channel)
      r (enqueue-tuples c [{:key "abc" :value "123"} 
                           {:key "def" :value "456"}])
      s (channel-seq c)]
  (fact s => 
        [["VALUE" "abc" "0" "123"] ["VALUE" "def" "0" "456"]]))


(let [c (channel)
      r (cmd->dispatch "get" c {} server-get-test 
                 (fn [cmd p] {:values [{:key "abc" :value "123"} 
                                   {:key "def" :value "456"}]}))
      s (channel-seq c)]
  (fact s =>  
        [["VALUE" "abc" "0" "123"] ["VALUE" "def" "0" "456"] ["END"]]))



(fact 
  (cmd->dispatch "get" [] {} server-get-test 
                 (fn [cmd p] {:error "something bad"})) => 
  ["SERVER_ERROR" "something bad"])

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
