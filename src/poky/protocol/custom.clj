(ns poky.protocol.custom
  (:use [poky.core :as poky]
        [lamina.core]
        [aleph.tcp]
        [gloss core io]))

(def TRM-DATA-BLOCK (string :utf-8 :suffix "\r\n"))
(def C (string :utf-8 :delimiters " "))
(def K (string :utf-8 :delimiters " "))
(def CR (string :utf-8 :delimiters ["\r\n"]))

(defcodec SET ["SET" CR])
(defcodec STORED ["STORED" CR])
;STORED\r\n

(defcodec GET ["GET" CR])
(defcodec GETS ["GETS" CR])
(defcodec END ["END" CR])
(defcodec VALUE [C CR CR])

; to test (decode GET-REPLY gets_test_b)
(defcodec GET-REPLY (repeated VALUE :prefix :none :delimiters ["END\r\n"]))

;VALUE <key> <flags> <bytes> [<cas unique>]\r\n
;<data block>\r\n
;...
;END\r\n

(defcodec ERRC (string :utf-8))

;(def sb (java.nio.ByteBuffer/wrap (.getBytes "STORED\r\nVALUE abc\r\n123\r\n")))
;(defcodec X ["STORED" CR C CR CR "END" CR])
;(def get_test_b (java.nio.ByteBuffer/wrap (.getBytes "VALUE abc\r\n123\r\nEND\r\n")))
;(def gets_test_b (java.nio.ByteBuffer/wrap (.getBytes "VALUE abc\r\n123\r\nVALUE cde\r\n456\r\nEND\r\n")))
;(def value_test_b (java.nio.ByteBuffer/wrap (.getBytes "VALUE abc\r\n123\r\n")))
;(def values_test_b (java.nio.ByteBuffer/wrap (.getBytes "VALUE abc\r\n123\r\nVALUE cde\r\n456\r\n")))

(defcodec CMDS
          (header C
                  (fn [h]
                    (case h
                      "SET" SET
                      "GET" GET
                      "GETS" GETS
                      "STORED" (do (println "stored") GET-REPLY)
                      (do (println "err") ERRC)))
                  first))

(defn cmd-args [decoded]
  (second decoded))

(defn extract-cmd-args [decoded f]
  (let [args (clojure.string/split (cmd-args decoded) #" ")]
    (if args
      (f args)
      nil)))

(defn cmd-set-key [decoded]
  (extract-cmd-args decoded first))

(defn cmd-set-value [decoded]
  (extract-cmd-args decoded second))

(defn cmd-get-key [decoded]
  (extract-cmd-args decoded first))

(defn cmd-gets-keys [decoded]
  (extract-cmd-args decoded (fn [v] v)))

(defn handle [ch ci cmd]
    (println "Processing command: " (first cmd) " from " ci)
    (condp = (first cmd)
      "SET" (do (println "doing enqueue put") (enqueue ch ["STORED"]))
      "GET" (enqueue ch "got get\r\n")
      "GETS" (enqueue ch "got mget\r\n")
      (enqueue ch "error")))


(defn handler
  [ch ci]
  (receive-all ch (partial handle ch ci)))


;(def s (start-tcp-server handler {:port 10000 :frame CMDS}))
