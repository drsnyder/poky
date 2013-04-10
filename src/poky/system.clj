(ns poky.system
  (:require [environ.core :refer [env]]
            [clojure.tools.cli :as cli]
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

(defn create
  [store server]
  (PokySystem. (atom {:store store :server server})))

(defn cli-runner [store app & args]
  (let [[opts args _] (cli/cli args
                           ["-p" "--port" "Listen on this port" :default 8080 :parse-fn #(Integer. %)]
                           ["-d" "--dsn" "Database DSN" :default (env :database-url "")])
        port          (:port opts)
        dsn           (:dsn opts)
        S (create (store dsn) app)]
    (println (format "Starting up on port %d." port))
    (start S port)))
