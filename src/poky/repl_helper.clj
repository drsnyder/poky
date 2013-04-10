(ns poky.repl-helper
  (:require 
    (poky.kv [memory :as m]
             [core :refer :all])
    [poky.kv.jdbc.util :as jdbc-util]
    [poky.kv.jdbc.text :as text]
    [poky.protocol.http.jdbc.text :as http]
    [poky.system :as system]
    [environ.core :refer [env]]
    [cheshire.core :as json]
    [midje.repl :refer :all]
    [clojure.java.jdbc :as sql]))

(def S (system/create (text/create (env :database-url)) #'http/api))

; (system/start S 8080)
; (system/stop S)

