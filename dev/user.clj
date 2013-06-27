(ns user
  (:require (poky.kv [jdbc :as kv.jdbc]
                     [core :refer :all])
            [poky.kv.jdbc.util :as jdbc-util]
            [poky.protocol.http :as http]
            [poky.system :as system]
            [poky.util :as util]
            [environ.core :refer [env]]
            [cheshire.core :as json]
            [clj-http.client :as client]
            [midje.sweet]
            [midje.repl :refer :all]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clj-time.format :as tf]
            [clojure.java.jdbc :as sql]
            [clojure.repl :refer [doc source]]
            [clojure.tools.namespace.repl :refer  (refresh refresh-all)])
  (:import java.nio.ByteBuffer))

(def S nil)

(if-not (env :database-url)
  (println "WARNING: Missing DATABASE_URL environment config. System may not initialize correctly."))

(defn init
  "Constructs the current development system"
  []
  (alter-var-root
    #'S
    (constantly
      (system/create-system (kv.jdbc/create (env :database-url))
                            http/start-server))))

(defn start
  "Starts the current development system"
  []
  (alter-var-root #'S #(system/start % (env :port 8080))))

(defn stop
  "Shutsdown and destroys current development system"
  []
  (alter-var-root #'S (fn [s] (when s (system/stop s)))))

(defn go
  "Initializes current development system and starts it running"
  []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))
