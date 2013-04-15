(ns poky.protocol.memcache.jdbc.text
  (:require [poky.protocol.memcache :as memcache]
            [poky.kv.core :as kv]
            [poky.util :refer :all]
            [lamina.core :refer :all]
            [aleph.tcp :refer :all]))

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
  (nth decoded 4))

(defn cmd-gets-keys [decoded]
  (clojure.string/split (first (rest decoded)) #" "))

(defn cmd-delete-key [decoded]
  (clojure.string/trim (second decoded)))

(defn create-tuple [k value]
  {:key k :value value})

(defn tuple-key [t]
  (:key t))

(defn tuple-value [t]
  (:value t))

(defn response-values [r]
  (:values r))

(defn create-response [tuples]
  {:values tuples})

(defn enqueue-tuples [ch tuples]
  (doall 
    (map #(enqueue ch %)
       (map (fn [t] ["VALUE" (:key t) "0" (:value t)]) tuples))))


(defmulti cmd->dispatch
  (fn [cmd kvstore channel client-info payload process-fn] (cmd-to-keyword cmd)))


(defmethod cmd->dispatch :set
  [cmd kvstore channel client-info payload process-fn] 
  (let [response (process-fn cmd kvstore payload)]
    (cond
      (or (:update response) (:insert response)) (enqueue channel ["STORED"])
      (:error response) (enqueue channel ["SERVER_ERROR" (:error response)])
      :else (enqueue channel ["SERVER_ERROR" "oops, something bad happened while setting."]))))

(defmethod cmd->dispatch :get
  [cmd kvstore channel client-info payload process-fn] 
  (let [response (process-fn cmd kvstore payload)]
    (cond 
      (:values response)
      (if (> (count (:values response)) 0)
        (do 
          (enqueue-tuples channel (:values response))
          (enqueue channel ["END"]))
        (enqueue channel ["END"]))
      (:error response) (enqueue channel ["SERVER_ERROR" (:error response)])
      :else (enqueue channel ["SERVER_ERROR" "oops, something bad happened while getting."]))))

(defmethod cmd->dispatch :gets
  [cmd kvstore channel client-info payload process-fn] 
  (cmd->dispatch :get channel client-info payload process-fn))

(defmethod cmd->dispatch :delete
  [cmd kvstore channel client-info payload process-fn] 
  (let [response (process-fn cmd kvstore payload)]
    (cond 
      (:deleted response) (enqueue channel ["DELETED"])
      (:error response) (enqueue channel ["SERVER_ERROR" (:error response)])
      :else (enqueue channel ["SERVER_ERROR" "oops, something bad happened while deleting."]))))


; is this a client error or just error?
(defmethod cmd->dispatch :default
  [cmd kvstore channel client-info payload process-fn] 
  (enqueue channel ["ERROR"]))



(defmulti storage->dispatch
  (fn [cmd kvstore req] (cmd-to-keyword cmd)))

(defmethod storage->dispatch :set
  [cmd kvstore req] 
  {:pre [(= (count req) 5)]}
  (let [ret (kv/set* kvstore
                     (cmd-set-key req)
                     (cmd-set-value req))]
    (cond 
      (map? ret) {:insert true}
      (seq? ret) {:update true}
      :else {:error "unknown update type"})))

(defmethod storage->dispatch :get
  [cmd kvstore req]
  {:pre [(= (count req) 2)]}
  (let [k (cmd-gets-keys req)
        ret (kv/mget* kvstore (cmd-gets-keys req))
        tuples (for [r ret] (create-tuple (first r) (second r)))]
    (create-response tuples)))


(defmethod storage->dispatch :gets
  [cmd kvstore req]
  {:pre [(= (count req) 2)]}
  (storage->dispatch :get req))

(defmethod storage->dispatch :delete
  [cmd kvstore req]
  {:pre [(= (count req) 2)]}
  {:deleted (first (kv/delete* kvstore (cmd-delete-key req)))})

(defmethod storage->dispatch :default
  [cmd kvstore req]
  {:error (format "Unknown storage command %s." cmd)})


(defn memcache-handler [kvstore ch ci cmd]
  (let [cmd-key (first cmd)]
    (cmd->dispatch cmd-key kvstore ch ci cmd
                   storage->dispatch)))

(defn api
  [kvstore ch ci]
  (receive-all ch (partial memcache-handler kvstore ch ci)))

(defn start-server [kvstore port]
  (start-tcp-server (partial api kvstore) {:port port :frame (memcache/memcache-codec :utf-8)}))

