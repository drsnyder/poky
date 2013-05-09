(ns poky.protocol.http.jdbc.text.main
  (:gen-class)
  (:require [poky.kv.jdbc :as jdbc]
            [poky.protocol.http.jdbc.text :as http]
            [poky.system :as system]))

(defn -main [& args]
  (apply system/cli-runner jdbc/create #'http/start-server args))

