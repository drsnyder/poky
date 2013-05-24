(ns poky.repl-helper
  (:use [clojure.repl :only (doc source)])
  (:require (poky.kv [jdbc :as kv.jdbc]
                     [core :refer :all])
            [poky.kv.jdbc.util :as jdbc-util]
            [poky.protocol.http :as http]
            [poky.system :as system]
            [poky.util :as util]
            [environ.core :refer [env]]
            [cheshire.core :as json]
            [midje.repl :refer :all]
            [clojure.java.jdbc :as sql])
  (:import java.nio.ByteBuffer))

(def S (delay (when-let [dsn (env :database-url)] (system/create-system (kv.jdbc/create dsn) #'http/start-server))))

; (system/start @S 8080)
; (system/stop @S)
