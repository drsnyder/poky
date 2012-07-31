(ns poky.repl-helper
    (:use [clojure.java.jdbc :as sql :only [with-connection]]
          [clojure.pprint]
          [poky db core]
          [poky.protocol.custom]
          [lamina.core]
          [aleph.tcp]
          [gloss core io])
    ;(:import com.mchange.v2.c3p0.ComboPooledDataSource)
    (:import java.nio.ByteBuffer)
    (:import [java.lang.reflect Method]))

; create table poky ( key varchar(1024) not null, value text, constraint thekey primary key (key) );
; export DATABASE_URL=postgresql://drsnyder@localhost:5432/somedb 
(def conn (get (System/getenv) "DATABASE_URL")) 

(def p_test_b (java.nio.ByteBuffer/wrap (.getBytes "PUT abc 123\r\n")))
(def g_test_b (java.nio.ByteBuffer/wrap (.getBytes "GET abc\r\n")))
(def m_test_b (java.nio.ByteBuffer/wrap (.getBytes "MGET abc def ghi\r\n")))
(def ms_test_b (java.nio.ByteBuffer/wrap (.getBytes "MGET abc\r\n")))

(defn test-decode []
  (do
    (println (decode CMDS p_test_b))
    (println (decode CMDS g_test_b))
    (println (decode CMDS m_test_b))
    (println (decode CMDS ms_test_b))))

(defn thandle [ch s] 
  (enqueue ch (format "You said %s which is a %s" s (type s))))

(defn ehandler [ch client-info]
  (receive-all ch (partial thandle ch)))

(start-tcp-server ehandler {:port 10001, :frame (string :utf-8 :delimiters ["\r\n"])})
