(defproject org.clojars.drsnyder/poky "1.0.0-SNAPSHOT"
  :description "A key-value store built on PostgreSQL."
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.cli "0.2.2"]
                 [postgresql/postgresql "9.0-801.jdbc4"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [c3p0/c3p0 "0.9.1.2"]
                 [clj-logging-config "1.9.10"]
                 [org.clojure/tools.logging "0.2.6"]
                 [ring/ring-core "1.1.7"]
                 [ring/ring-jetty-adapter "1.1.0"]
                 [ring-middleware-format "0.2.3"]
                 [compojure "1.1.3"]
                 [clj-stacktrace "0.2.5"]
                 [environ "0.2.1"]]
  :profiles {:dev {:dependencies [[midje "1.5.1"]]
                   :plugins [[lein-midje "3.0.0"]]}}
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"
            :distribution :repo
            :comments "MIT"}
  :repl-options {:init-ns poky.repl-helper}
  :main poky.protocol.http.main
  :uberjar-name "poky-standalone.jar"
  :jvm-opts ["-server"])
