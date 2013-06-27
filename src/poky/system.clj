(ns poky.system
  (:require [environ.core :refer [env]]
            [poky.kv.jdbc :refer [get-connection]]
            [poky.kv.jdbc.util :refer [close-connection]]
            [ring.middleware.statsd :as statsd]
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
             (server store port))
      this))
  (stop [this]
    (-> this store get-connection close-connection)
    (.stop (:running @state))
    this))

(defn create-system
  [store server]
  (->PokySystem (atom {:store store :server server})))

(defn cli-runner [store app & args]
  (let [[opts args banner] (cli/cli args
                                    ["-p" "--port" "Listen on this port" :default 8080 :parse-fn #(Integer. %)]
                                    ["-d" "--dsn" "Database DSN" :default (env :database-url "")]
                                    ["--statsd-host" "Statsd host:port" :default (env :statsd-host)])
        port (:port opts)
        dsn (:dsn opts)]

    (when (empty? dsn)
      (println "--dsn is required.")
      (println banner)
      (System/exit 1))

    (when-let [statsd-host (:statsd-host opts)]
      (let [[host port] (clojure.string/split statsd-host #":")
            port (Integer/parseInt port)]
        (when (and host port)
          (println "Sending statsd metrics to" statsd-host)
          (statsd/setup! host port))))

    (println (format "Starting up on port %d." port))
    (start (create-system (store dsn) app) port)))
