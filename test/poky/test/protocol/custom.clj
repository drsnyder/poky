(ns poky.test.protocol.custom
  (:use [poky.protocol custom]
        ;[lamina.core]
        [gloss core io]
        [clojure.test]
        [midje.sweet]))

(def set-tests [(java.nio.ByteBuffer/wrap (.getBytes "SET abc 123\r\n"))])
(def get-tests [(java.nio.ByteBuffer/wrap (.getBytes "GET abc\r\n"))])
(def gets-tests [(java.nio.ByteBuffer/wrap (.getBytes "GETS abc def ghi\r\n"))])

(fact 
  (cmd-set-key ["SET" "abc 123"]) => "abc")

(fact 
  (cmd-set-value ["SET" "abc 123"]) => "123")

(fact 
  (cmd-get-key ["GET" "abc"]) => "abc")

(fact 
  (first (decode CMDS (first set-tests))) => "SET")

(fact 
  (cmd-set-key (decode CMDS (first set-tests))) => "abc")

(fact 
  (cmd-set-value (decode CMDS (first set-tests))) => "123")

(fact 
  (first (decode CMDS (first gets-tests))) => "GETS")

(fact
  (first (cmd-gets-keys (decode CMDS (first gets-tests)))) => "abc")

(fact
  (second (cmd-gets-keys (decode CMDS (first gets-tests)))) => "def")

