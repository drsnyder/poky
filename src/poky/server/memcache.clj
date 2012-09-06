(ns poky.server.memcache
  (:use [lamina.core]
        [aleph.tcp])
  (:require [poky.core :as poky]
            [poky.protocol.memcache :as pm]))

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
  (clojure.string/split (first (rest decoded)) #" "))


(defn memcache [ch ci cmd]
    (println "Processing command: " (first cmd) " from " ci)
    (condp = (first cmd)
      "set" (enqueue ch ["STORED"]) 
      "get" (do (enqueue ch ["VALUE" "abc" "0" "3" "123"]) (enqueue ch ["END"]))
      "gets" (do (enqueue ch ["VALUE" "abc" "0" "3" "123"]) (enqueue ch ["END"]))
      (enqueue ch "error")))


(defn handler
  [ch ci]
  (receive-all ch (partial memcache ch ci)))

(defn start-server [port]
  (start-tcp-server handler {:port port :frame (pm/memcache-codec :utf-8)}))
