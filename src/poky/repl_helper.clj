(ns poky.repl-helper
  (:require 
    (poky.kv [memory :as m]
             [jdbc :as kvstore]
             [core :refer :all])
    [poky.kv.jdbc.util :as jdbc-util]
    [poky.system :as system]
    [poky.protocol.http :as http]
    [environ.core :refer [env]]
    [cheshire.core :as json]
    [midje.repl :refer :all]))

(def S (system/create (kvstore/create (env :database-url)) #'http/api))
