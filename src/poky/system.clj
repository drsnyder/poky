(ns poky.system
  (:require [environ.core :refer [env]]
            [ring.adapter.jetty :as jetty]))

(def ^:private default-jetty-max-threads 25)

(defprotocol SystemAPI 
  (store [this])
  (start [this port])
  (stop [this]))

(defrecord PokySystem [state]
  SystemAPI
  (store [this] 
    (:store @state))
  (start [this port]
    (swap! state assoc :running
           (jetty/run-jetty ((:server @state) (:store @state))
                            {:port port
                             :max-threads default-jetty-max-threads
                             :join? false})))
  (stop [this]
    (.stop (:running @state))))

; add the http handler here to and stop and start
(defn create
  [store server]
  (PokySystem. (atom {:store store :server server})))
