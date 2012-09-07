(defproject org.clojars.drsnyder/poky "1.0.0-SNAPSHOT"
  :description "PostgreSQL key value store"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/tools.cli "0.2.1"]
                 [postgresql "8.4-702.jdbc4"]
                 [org.clojure/java.jdbc "0.1.4"]
                 [c3p0/c3p0 "0.9.1.2"] 
                 [gloss "0.2.2-SNAPSHOT"]
                 [aleph "0.3.0-SNAPSHOT"]
                 [slingshot "0.10.3"]
                 [ring/ring-core "[0.2.0,)"]
                 [ring-json-params "0.1.3"]
                 [compojure "1.1.1"]
                 [org.clojure/data.json "0.1.2"]]
  :dev-dependencies [[midje "1.4.0"] 
                     [lein-ring "0.5.4"]
                     [lein-midje "1.0.10"]
                     [com.stuartsierra/lazytest "1.2.3"]]
  :ring {:handler poky.protocol.http/http-app}
  :repositories {"stuart" "http://stuartsierra.com/maven2"}
  :repl-init poky.repl-helper
  :test-selectors {:default (complement :integration)
                   :integration :integration
                   :all (fn [_] true)})
