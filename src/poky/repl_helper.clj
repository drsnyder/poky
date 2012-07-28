(ns poky.repl-helper
    (:use [clojure.java.jdbc :as sql :only [with-connection]]
          [clojure.pprint]
          [poky db core]
          [poky.protocol.custom]
          [lamina.core]
          [aleph.tcp]
          [gloss core io])
    (:import com.mchange.v2.c3p0.ComboPooledDataSource)
    (:import java.nio.ByteBuffer)
    (:import [java.lang.reflect Method]))

; create table poky ( key varchar(1024) not null, value text, constraint thekey primary key (key) );
; export DATABASE_URL=postgresql://drsnyder@localhost:5432/somedb 
(def conn (get (System/getenv) "DATABASE_URL")) 

(def putb (java.nio.ByteBuffer/wrap (.getBytes "PUT abc 123\r\n"))) 
(def mgetb (java.nio.ByteBuffer/wrap (.getBytes "MGET abc cde\r\n"))) 

