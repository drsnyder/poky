(ns poky.main
  (:gen-class)
  (:require 
    (poky.kv [memory :as m]
             [jdbc :as kvstore]
             [core :refer :all])
    [poky.kv.jdbc.util :as jdbc-util]
    [poky.kv.jdbc.text :as text]
    [poky.protocol.http.jdbc.text :as http]
    [poky.system :as system]
    [environ.core :refer [env]]))

(defn -main [& args]
  (let [[opts args _] (cli args
                           #_"Poky server"
                           ["-p" "--port" "Listen on this port" :default 8080 :parse-fn #(Integer. %)])
        port          (:port opts)]
    (println opts)
    (println args)
    (println (format "Starting up on port %d." port))
    (poky/start-server port)))

