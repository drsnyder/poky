(ns poky.protocol.http.jdbc.text.main
  (:gen-class)
  (:require 
    [poky.kv.jdbc.text :as text]
    [poky.protocol.http.jdbc.text :as http]
    [poky.system :as system]
    [environ.core :refer [env]]))

(defn -main [& args]
  (apply (partial system/cli-runner text/create #'http/api) args))

