(defproject org.clojars.drsnyder/poky "1.0.0-SNAPSHOT"
  :description "PostgreSQL key value store"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/tools.cli "0.2.1"]
                 [postgresql "8.4-702.jdbc4"]
                 [org.clojure/java.jdbc "0.1.4"]
                 [c3p0/c3p0 "0.9.1.2"] 
                 ;[aleph "0.3.0-alpha2"]
                 [aleph "0.2.1-rc5"]
                 ]
  :dev-dependencies [[midje "1.3.1"]]
  :repl-init poky.repl-helper
  :test-selectors {:default (complement :integration)
                   :integration :integration
                   :all (fn [_] true)})
