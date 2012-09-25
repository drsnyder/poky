(ns poky.main
  (:gen-class)
  (:use clojure.tools.cli)
  (:require [poky.server.memcache :as poky]
            [poky.vars :as config]))


(defn -main [& args]
  (let [[opts args _] (cli args
                           #_"Poky server"
                           ["-h" "--help" "Show help" :default false :flag true]
                           ["-p" "--port" "Listen on this port" :default 11219 :parse-fn #(Integer. %)]
                           ["-u" "--jdbc-url" "JDBC url" :default config/*jdbc-url*]
                           ["-U" "--user" "User to connect with" :default config/*user*]
                           ["-P" "--password" "Password to connect with" :default config/*password*]
                           ["-t" "--table" "Table to use." :default config/*table*])
        help          (:help opts)
        port          (:port opts)
        jdbc-url      (:jdbc-url opts)
        user          (:user opts)
        password      (:password opts)
        password      (:table opts)]

    (poky/set-config! jdbc-url user password table)
    (poky/connect!)
    (poky/start-server port)))

