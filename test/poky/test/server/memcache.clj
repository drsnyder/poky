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


(let [c (channel)
      r (enqueue-tuples c [{:key "abc" :value "123"} 
                           {:key "def" :value "456"}])
      s (channel-seq c)]
  (fact s => 
        [["VALUE" "abc" "0" "123"] ["VALUE" "def" "0" "456"]]))


(defn pass-through-cmd->dispatch-test [cmd in cb expected-out]
  (let [c (channel)
        r (cmd->dispatch cmd c {} in cb)
        s (channel-seq c)]
    (fact expected-out => s)))

(pass-through-cmd->dispatch-test "set" server-set-test 
                   (fn [cmd p] {:update true}) 
                                 (list ["STORED"]))

(pass-through-cmd->dispatch-test "set" server-set-test 
                   (fn [cmd p] {:insert true}) 
                                 (list ["STORED"]))

(pass-through-cmd->dispatch-test "set" server-set-test 
                 (fn [cmd p] {:error "an error"}) 
                                 (list ["SERVER_ERROR" "an error"]))

(pass-through-cmd->dispatch-test "set" server-set-test 
                 (fn [cmd p] {})
                                 (list ["SERVER_ERROR" "oops, something bad happened while setting."]))

(pass-through-cmd->dispatch-test "set" server-set-test 
                 (fn [cmd p] {:error "something bad"}) 
                                 (list ["SERVER_ERROR" "something bad"]))

(pass-through-cmd->dispatch-test "get" server-get-test 
                 (fn [cmd p] {:values [{:key "abc" :value "123"} 
                                   {:key "def" :value "456"}]})
                                 [["VALUE" "abc" "0" "123"] ["VALUE" "def" "0" "456"] ["END"]])



(pass-through-cmd->dispatch-test "get" server-get-test 
                 (fn [cmd p] {:error "something bad"})
                                 (list ["SERVER_ERROR" "something bad"]))

(pass-through-cmd->dispatch-test "get" server-get-test 
                                 (fn [cmd p] {})
                                 (list ["SERVER_ERROR" "oops, something bad happened while getting."]))

(pass-through-cmd->dispatch-test "delete" server-delete-test 
                 (fn [cmd p] {:deleted 1})
                                 (list ["DELETED"]))

(pass-through-cmd->dispatch-test "delete" server-delete-test 
                                 (fn [cmd p] {:error "something bad"}) 
                                 (list ["SERVER_ERROR" "something bad"]))

(pass-through-cmd->dispatch-test "delete" server-delete-test 
                                 (fn [cmd p] {}) 
                                 (list ["SERVER_ERROR" "oops, something bad happened while deleting."]))
