(ns poky.protocol.custom
  (:use [poky.core :as poky]
        [lamina.core]
        [aleph.tcp]
        [gloss core io]))

(def K (string :utf-8 :delimiters " "))
(def V (string :utf-8 :delimiters " "))
(def CR (string :utf-8 :delimiters ["\r\n"]))

(defcodec PUT ["PUT" K CR])
(defcodec GET ["GET" V CR])
(defcodec MGET ["MGET" K CR])
(defcodec ERRC (string :utf-8))

(defcodec CMDS (header K (fn [h] 
                           (condp = h
                             "PUT" PUT
                             "GET" GET
                             "MGET" MGET
                             ERRC)) 
                       (fn [b] (first b))))

(defn handler
  "TCP Handler. Decodes the issued command and calls the appropriate
  function to excetion some action."
  [ch ci]
  (receive-all ch
               (fn [b]

                 (let [deced (decode CMDS b)]
                   (println "Processing command: " deced)
                   (condp = (first deced)
                     "PUT" (enqueue ch "got put")
                     "GET" (enqueue ch "got get")
                     "MGET" (enqueue ch "got mget")
                     (enqueue ch "error"))))))

(defn shandler [ch info]
  (receive-all ch (fn [d] (println d) (enqueue ch d))))

;(start-tcp-server handler {:port 10000})
