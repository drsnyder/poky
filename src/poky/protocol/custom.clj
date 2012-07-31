(ns poky.protocol.custom
  (:use [poky.core :as poky]
        [lamina.core]
        [aleph.tcp]
        [gloss core io]))

(def C (string :utf-8 :delimiters " "))
(def CR (string :utf-8 :delimiters ["\r\n"]))

(defcodec PUT ["PUT" CR])
(defcodec GET ["GET" CR])
(defcodec MGET ["MGET" CR])
(defcodec ERRC (string :utf-8))



(defcodec CMDS
          (header C
                  (fn [h]
                    (case h
                          "PUT" PUT
                          "GET" GET
                          "MGET" MGET
                          ERRC))
                  first))

(defn handle [ch ci cmd]
    (println "Processing command: " (first cmd) " from " ci)
    (condp = (first cmd)
      "PUT" (do (println "doing enqueue put") (enqueue ch "got put\r\n"))
      "GET" (enqueue ch "got get\r\n")
      "MGET" (enqueue ch "got mget\r\n")
      (enqueue ch "error")))


(defn handler
  [ch ci]
  ;(receive-all ch (partial handle ch ci)))
  (fn [cmd]
    (println "Processing command: " (first cmd) " from " ci)
    (condp = (first cmd)
      "PUT" (enqueue ch ["got put"])
      "GET" (enqueue ch "got get\r\n")
      "MGET" (enqueue ch "got mget\r\n")
      (enqueue ch "error"))))


;(def s (start-tcp-server handler {:port 10000 :frame CMDS}))
