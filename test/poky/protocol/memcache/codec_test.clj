(ns poky.protocol.memcache.codec-test
  (:use [poky.protocol.memcache.codec]
        [gloss core io]
        [clojure.test]
        [midje.sweet]))

(def set-tests [(java.nio.ByteBuffer/wrap (.getBytes "set abc 0 0 3\r\n123\r\n"))])
(def get-tests [(java.nio.ByteBuffer/wrap (.getBytes "get abc def\r\n"))])
(def gets-tests [(java.nio.ByteBuffer/wrap (.getBytes "gets abc def ghi jkl\r\n"))])
(def delete-tests [(java.nio.ByteBuffer/wrap (.getBytes "delete abc\r\n"))])
(def deleted-tests [(java.nio.ByteBuffer/wrap (.getBytes "DELETED\r\n"))])
(def value-tests [(java.nio.ByteBuffer/wrap (.getBytes "VALUE abc 0 3\r\n123\r\n"))])
(def end-tests [(java.nio.ByteBuffer/wrap (.getBytes "END\r\n"))])
(def stored-tests [(java.nio.ByteBuffer/wrap (.getBytes "STORED\r\n"))])

(def MEMCACHE (memcache-codec :utf-8))


(fact 
  (first (decode MEMCACHE (first set-tests))) => "set")


(fact 
  (first (decode MEMCACHE (first get-tests))) => "get")

(fact 
  (second (decode MEMCACHE (first get-tests))) => "abc def")

(fact 
  (first (decode MEMCACHE (first gets-tests))) => "gets")

(fact 
  (second (decode MEMCACHE (first value-tests))) => "abc")

(fact 
  (first (decode MEMCACHE (first end-tests))) => "END")

(fact 
  (first (decode MEMCACHE (first stored-tests))) => "STORED")

(fact 
  (first (decode MEMCACHE (first delete-tests))) => "delete")

(fact 
  (second (decode MEMCACHE (first delete-tests))) => "abc")

(fact 
  (first (decode MEMCACHE (first deleted-tests))) => "DELETED")

