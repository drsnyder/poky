(ns poky.test.protocol.custom
  (:use [poky.protocol.custom]
        [lamina.core]
        [gloss core io]
        [clojure.test]
        [midje.sweet]))

(def set-tests [(java.nio.ByteBuffer/wrap (.getBytes "SET abc 123\r\n"))])
(def get-tests [(java.nio.ByteBuffer/wrap (.getBytes "GET abc\r\n"))])
(def gets-tests [(java.nio.ByteBuffer/wrap (.getBytes "GETS abc def ghi\r\n"))])


(fact 
  (first (decode CMDS (first set-tests))) => "SET")

(fact 
  (second (decode CMDS (first set-tests))) => "abc")
