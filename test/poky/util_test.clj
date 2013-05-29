(ns poky.util-test
  (:require [poky.util :as util]
            [environ.core :refer [env]]
            [midje.sweet :refer :all]))

(facts :first=
       (util/first= '(0) 1) => falsey
       (util/first= '(1) 1) => truthy
       (util/first= nil 1) => falsey)
