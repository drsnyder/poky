(defproject org.clojars.drsnyder/poky "1.2.0"
  :description "A key-value store built on PostgreSQL."
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.cli "0.2.2"]
                 [postgresql/postgresql "9.0-801.jdbc4"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [cheshire "5.2.0"]
                 [c3p0/c3p0 "0.9.1.2"]
                 [clj-logging-config "1.9.10"]
                 [org.clojure/tools.logging "0.2.6"]
                 [ring/ring-core "1.1.7"]
                 [ring/ring-jetty-adapter "1.1.0"]
                 [ring-middleware-format "0.2.3"]
                 [ring.middleware.statsd "1.0.0"]
                 [compojure "1.1.3"]
                 [clj-http "0.7.2"]
                 [clj-stacktrace "0.2.5"]
                 [clj-time "0.5.1"]
                 [environ "0.2.1"]
                 [com.newrelic.agent.java/newrelic-api "2.19.1"]]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[midje "1.5.1"]
                                  [org.clojure/tools.nrepl "0.2.3"]
                                  [org.clojure/tools.namespace "0.2.3"]]
                   :plugins [[lein-midje "3.0.0"]]}}
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"
            :distribution :repo
            :comments "MIT"}
  :repl-options {:init-ns user}
  :main poky.protocol.http.main
  :uberjar-name "poky-standalone.jar"
  :jvm-opts ["-server"])
