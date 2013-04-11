(defproject org.clojars.drsnyder/poky "1.0.0-SNAPSHOT"
  :description "A key-value store built on PostgreSQL."
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/tools.cli "0.2.2"] 
                 [postgresql/postgresql "9.0-801.jdbc4"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [c3p0/c3p0 "0.9.1.2"] 
                 [gloss "0.2.2-SNAPSHOT"]
                 [aleph "0.3.0-SNAPSHOT"]
                 [ring/ring-core "1.1.7"]
                 [ring/ring-jetty-adapter "1.1.0"]                                              
                 [ring-middleware-format "0.2.3"]       
                 [compojure "1.1.3"]
                 [environ "0.2.1"]]
  :profiles {:dev {:dependencies [[midje "1.5.1"]]
                   :plugins [[lein-midje "3.0.0"]]}}
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :repl-options {:init-ns poky.repl-helper}
  :main poky.protocol.http.jdbc.text.main
  :uberjar-name "poky-standalone.jar"
  :jvm-opts ["-server"])
