(ns poky.server.memcache
  (:use [poky util core]
        [lamina.core]
        [aleph.tcp])
  (:require [poky.protocol.memcache :as pm]))

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





(defmulti cmd->dispatch
  (fn [cmd channel client-info payload process-fn] cmd))

(defmethod cmd->dispatch "set" 
  [cmd channel client-info payload process-fn] 
  (let [response (process-fn payload)]
    (if (:error response)
      (enqueue channel ["SERVER_ERROR" (:error response)])
      (enqueue channel ["STORED"]))))

(defmethod cmd->dispatch "get" 
  [cmd channel client-info payload process-fn] 
  (let [response (process-fn payload)]
    (if (:error response)
      (enqueue channel ["SERVER_ERROR" (:error response)])
      (enqueue channel 
               (flatten
                 (concat 
                   (map 
                     (fn [t]
                       ["VALUE" (:key t) "0" "0" (str (count (:value t))) (:value t)]) 
                     (:values response)) ["END"]))))))


(defn memcache-handler [ch ci handler-map cmd]
  (let [cmd-key (cmd-to-keyword (first cmd))]
    (cmd->dispatch cmd-key ch ci (rest cmd) 
                   (get handler-map cmd-key (get handler-map :error)))))

  ;(condp = (first cmd)
  ;  "set" (enqueue ch ["STORED"]) 
  ;  "get" (do (enqueue ch ["VALUE" "abc" "0" "3" "123"]) (enqueue ch ["END"]))
  ;  "gets" (do (enqueue ch ["VALUE" "abc" "0" "3" "123"]) (enqueue ch ["END"]))
  ;  (enqueue ch "error")))

;(defmulti dispatch (fn [cmd payload] cmd))
;(defmethod dispatch "set" 
;  [cmd payload] 
;  (format "recieved set x => %s %s" (first payload) (second payload)))
;
;(dispatch "set" ["abc" "123"])

(defn handler
  [ch ci]
  (receive-all ch (partial memcache-handler ch ci)))

(defn start-server [port]
  (start-tcp-server handler {:port port :frame (pm/memcache-codec :utf-8)}))
