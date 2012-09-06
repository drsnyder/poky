(ns poky.test.server.memcache
  (:use [poky.server.memcache]
        [clojure.test]
        [midje.sweet]))

(def server-set-test ["SET" "abc" "0" "0" "3" "123"])
(def server-get-test ["get" "abc def"])

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

