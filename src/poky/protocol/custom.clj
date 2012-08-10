(ns poky.protocol.custom
  (:use [poky.core :as poky]
        [lamina.core]
        [aleph.tcp]
        [gloss core io]))

(def TRM-DATA-BLOCK (string :utf-8 :suffix "\r\n"))
(def C (string :utf-8 :delimiters [" "]))
(def NDLM (string :utf-8))
(def K (string :utf-8 :delimiters " "))
(def V (string :utf-8 :delimiters " "))
(def CR (string :utf-8 :delimiters ["\r\n"]))

(def COMMAND (string :utf-8 :delimiters ["\r\n" " "]))

(defcodec SET ["SET" C CR])
(defcodec STORED ["STORED"])
;STORED\r\n

(defcodec GET ["GET" CR])
(defcodec GETS ["GETS" CR])

(defcodec VALUE ["VALUE" CR CR])
(defcodec END ["END"])

;VALUE <key> <flags> <bytes> [<cas unique>]\r\n
;<data block>\r\n
;...
;END\r\n

(defcodec ERRC (string :utf-8))

;(def store_test_b (java.nio.ByteBuffer/wrap (.getBytes "STORED\r\n")))
;(def get_test_b (java.nio.ByteBuffer/wrap (.getBytes "VALUE abc\r\n123\r\n")))
;(def value_test_b (java.nio.ByteBuffer/wrap (.getBytes "VALUE abc\r\n123\r\n")))


(defcodec commands
          (header (string :utf-8 :delimiters ["" " "])
                  (fn [hd]
                    (println "h->b " hd)
                    (case hd
                      "" (compile-frame (string :utf-8 :delimiters ["\r\n"]))
                      "SET " SET
                      "VALUE " VALUE))
                  (fn [body] 
                    (let [bkey (if (vector? body) (first body) body)]
                      (println "b->h " body " " bkey " " (type body))
                      (case bkey
                        "END" ""
                        "STORED" ""
                        "SET" "SET "
                        "VALUE" "VALUE ")))))

;(frame-to-string (encode commands "END"))
;(frame-to-string (encode commands ["SET" "abc" "123"]))
;(frame-to-string (encode commands ["VALUE" "abc" "123"]))

(defcodec CMDS
          (header CR 
                  (fn [h]
                    (println "processing '" h "'")
                    (case h
                      "SET" SET
                      "GET" GET
                      "GETS" GETS
                      "STORED" STORED
                      "VALUE" (do (println "do value") VALUE)
                      "END" END
                      ERRC))
                  (fn [h] 
                    (println "processing header" h " " (type h)) 
                      (first h))))

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

(defn debug-response [ch msg r]
  (do 
    (println msg r)
    (map #(enqueue ch %) r)))

(defn test-handle [ch ci cmd]
    (println "Processing command: " (first cmd) " from " ci)
    (condp = (first cmd)
      "SET" (enqueue ch ["STORED"]) 
      "GET" (do (enqueue ch ["VALUE" "abc" "123"]) (enqueue ch ["END" ""]))
      "GETS" (do (enqueue ch ["VALUE" "abc" "123"]) (enqueue ch ["END" ""]))
      (enqueue ch "error")))


(defn handler
  [ch ci]
  (receive-all ch (partial test-handle ch ci)))


;(def s (start-tcp-server handler {:port 10000 :frame CMDS}))
