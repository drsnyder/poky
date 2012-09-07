(ns poky.server.memcache
  (:use [poky util]
        [lamina.core]
        [aleph.tcp])
  (:require [poky.protocol.memcache :as pm]
            [poky.core :as poky]))

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
  (fn [cmd channel client-info payload process-fn] (cmd-to-keyword cmd)))

(defmethod cmd->dispatch :set
  [cmd channel client-info payload process-fn] 
  (let [response (process-fn payload)]
    (if (:error response)
      (enqueue channel ["SERVER_ERROR" (:error response)])
      (enqueue channel ["STORED"]))))

(defmethod cmd->dispatch :get
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

(defmethod cmd->dispatch :gets
  [cmd channel client-info payload process-fn] 
  (cmd->dispatch :get channel client-info payload process-fn))

(defn db->set
  [req]
  (poky/add 
    (cmd-set-key req)
    (cmd-set-value req)))

(defn db->gets
  [req]
  (poky/gets (cmd-gets-keys req)))

(def handler-map
  {:set db->set
   :get db->gets
   :gets db->gets})

(defn memcache-handler [ch ci handler-map cmd]
  (let [cmd-key (cmd-to-keyword (first cmd))]
    (cmd->dispatch cmd-key ch ci (rest cmd) 
                   (get handler-map cmd-key (get handler-map :error)))))

(defn handler
  [ch ci]
  (receive-all ch (partial memcache-handler ch ci handler-map)))

(defn start-server [port]
  (start-tcp-server handler {:port port :frame (pm/memcache-codec :utf-8)}))
