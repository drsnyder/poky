(ns poky.protocol.memcache
  (:use [poky.core :as poky]
        [lamina.core]
        [aleph.tcp]
        [gloss core io]))

(def C (string :utf-8 :delimiters [" "]))
(def K (string :utf-8 :delimiters [" "]))
(def V (string :utf-8 :delimiters [" "]))
(def CMD (string :utf-8 :delimiters " "))
(def CR (string :utf-8 :delimiters ["\r\n" " "]))

;set <key> <flags> <exptime> <bytes> [noreply]\r\n
;<data block>\r\n
(defcodec SET ["set" C C CR CR])

;STORED\r\n
(defcodec STORED ["STORED"])

;get <key>*\r\n
;gets <key>*\r\n
(defcodec GET ["get" (repeated K :delimiters ["\r\n"]) CR])
(defcodec GETS ["gets" (repeated K :delimiters [" "]) CR])

(defcodec VALUE ["VALUE" K V V CR CR])
;VALUE <key> <flags> <bytes> [<cas unique>]\r\n
;<data block>\r\n
;...

(defcodec END ["END"])
;END\r\n

(defcodec ERRC (string :utf-8))

(defn header->b [hd]
  (let [elems (clojure.string/split hd #" ")
        cmd (first elems)]
    (println (format "header->b '%s' '%s'" hd cmd))
    (case cmd
      "END" (compile-frame (string :utf-8 :delimiters ["\r\n"]))
      "STORED" (compile-frame (string :utf-8 :delimiters ["\r\n"]))
      (compile-frame (string :utf-8 :delimiters [" "])))))

(defn body->h [body] 
  (first body))



(defn h->b [hd] 
  "Called when decoding. Determines how to construct the body."
  (println (format "header h->b: '%s'" hd))
    (case hd
      "gets" GETS
      "get"  GET
      "VALUE" VALUE
      "VALU" (compile-frame ["VALUE" (string :utf-8 :delimiters ["E "]) K V V CR CR])
      "STORED" STORED
      "STOR" (compile-frame ["STORED" (string :utf-8 :delimiters ["ED\n"])])
      "END" END
      "END\r" (compile-frame ["END" (string :utf-8 :delimiters ["\n"])])
      "" (compile-frame [(string :utf-8 :delimiters ["\r\n"])])))



(defn b->h 
  "Called when encoding. Determines the header that is generated."
  [body]
  (println (format "b->h '%s'" body))
  (first body))

(defn memcache-pre-encode [req]
  (println (format "memcache-pre-encode '%s'" req))
  ; consider using finite-frame for STORED/END and add a pre/post method
  req)

(defn memcache-post-decode [res]
  (println (format "memcache-post-decode '%s'" res))
  res)

(defcodec MEMCACHE (compile-frame 
                     (header (string :utf-8 :delimiters ["\r\n" " "])
                             h->b
                             b->h)
                     memcache-pre-encode
                     memcache-post-decode))

(def cmd-codecs )


(def format-command 
  (enum 
    :byte 
    {:end \E
     :value \V
     :stored \S
     :get \g}))

(def codec-map {
                :end (compile-frame ["ND" CR])
                })

(defcodec TM (compile-frame
               (header format-command
                       codec-map
                       first)))




(defn cmd-args-len [decoded]
  (count (rest decoded)))

(defn cmd-args [decoded n]
  (nth decoded n))

(defn extract-cmd-args [decoded f]
  (let [args (clojure.string/split (cmd-args decoded) #" ")]
    (if args
      (f args)
      nil)))

(defn cmd-set-key [decoded]
  (second decoded))

(defn cmd-set-value [decoded]
  (nth decoded 5))

(defn cmd-gets-keys [decoded]
  (rest decoded))

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


;(def s (start-tcp-server handler {:port 10000 :frame MEMCACHE}))
