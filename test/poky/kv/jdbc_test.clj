(ns poky.kv.jdbc-test
  (:require [poky.kv.jdbc :as store]
            [environ.core :refer [env]]
            [midje.sweet :refer :all]))

(def bucket (.name *ns*))

(facts :jdbc :set :integration
       true => false)


