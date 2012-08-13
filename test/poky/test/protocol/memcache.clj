(ns poky.test.protocol.memcache
  (:use [poky.protocol.memcache]
        [gloss core io]
        [clojure.test]
        [midje.sweet]))

(def set-tests [(java.nio.ByteBuffer/wrap (.getBytes "set abc 0 0 3 123\r\n"))])
(def get-tests [(java.nio.ByteBuffer/wrap (.getBytes "get abc\r\n"))])
(def gets-tests [(java.nio.ByteBuffer/wrap (.getBytes "gets abc def ghi\r\n"))])
(def value-tests [(java.nio.ByteBuffer/wrap (.getBytes "VALUE abc 0 0 3\r\n123\r\n"))])
(def end-tests [(java.nio.ByteBuffer/wrap (.getBytes "END\r\n"))])
(def stored-tests [(java.nio.ByteBuffer/wrap (.getBytes "STORED\r\n"))])


(fact 
  (cmd-set-key ["SET" "abc" "123"]) => "abc")

(fact 
  (cmd-set-value ["SET" "abc" "0" "0" "3" "123"]) => "123")

;(fact 
;  (cmd-get-key ["GET" "abc"]) => "abc")

;(fact 
;  (first (decode MEMCACHE (first set-tests))) => "SET")
;
;(fact 
;  (cmd-set-key (decode MEMCACHE (first set-tests))) => "abc")
;
;(fact 
;  (cmd-set-value (decode MEMCACHE (first set-tests))) => "123")
;
;(fact 
;  (first (decode MEMCACHE (first gets-tests))) => "GETS")

;(fact
;  (first (cmd-gets-keys (decode MEMCACHE (first gets-tests)))) => "abc")
;
;(fact
;  (second (cmd-gets-keys (decode MEMCACHE (first gets-tests)))) => "def")

(fact 
  (second (decode MEMCACHE (first value-tests))) => "abc")

(fact 
  (first (decode MEMCACHE (first end-tests))) => "END")

(fact 
  (first (decode MEMCACHE (first stored-tests))) => "STORED")
