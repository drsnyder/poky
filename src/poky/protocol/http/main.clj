(ns poky.protocol.http.main
  (:gen-class)
  (:require [poky.kv.jdbc :as jdbc]
            [poky.protocol.http :as http]
            [poky.system :as system]))

(defn -main [& args]
  (apply system/cli-runner jdbc/create #'http/start-server args))

