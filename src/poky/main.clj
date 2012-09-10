(ns poky.main
  (:gen-class)
  (:use clojure.tools.cli)
  (:require [poky.server.memcache :as poky]))


(defn -main [& args]
  (let [[opts args _] (cli args
                           #_"Poky server"
                           ["-p" "--port" "Listen on this port" :default 11219 :parse-fn #(Integer. %)])
        port          (:port opts)]
    (println opts)
    (println args)
    (println (format "Starting up on port %d." port))
    (poky/start-server port)))

