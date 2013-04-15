(ns poky.system
  (:require [environ.core :refer [env]]
            [clojure.tools.cli :as cli]))


(defprotocol SystemAPI 
  (store [this])
  (start [this port])
  (stop [this]))

(defrecord PokySystem [state]
  SystemAPI
  (store [this] 
    (:store @state))
  (start [this port]
    (let [server (get @(:state this) :server)
          store (get @(:state this) :store)]
      (swap! state assoc :running
             (server store port))))
  (stop [this]
    (.stop (:running @state))))

(defn create-system
  [store server]
  (PokySystem. (atom {:store store :server server})))

(defn cli-runner [store app & args]
  (let [[opts args _] (cli/cli args
                           ["-p" "--port" "Listen on this port" :default 8080 :parse-fn #(Integer. %)]
                           ["-d" "--dsn" "Database DSN" :default (env :database-url "")])
        port          (:port opts)
        dsn           (:dsn opts)
        S (create-system (store dsn) app)]
    (println (format "Starting up on port %d." port))
    (start S port)))
