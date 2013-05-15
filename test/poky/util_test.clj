(ns poky.util-test
  (:require [poky.util :as util]
            [environ.core :refer [env]]
            [midje.sweet :refer :all]))


; FIXME
(facts :cmd-to-keyword
       (util/cmd-to-keyword "CMD") => false)


(facts :random-string :slow
       (let [set-size 1000
             iter     1000]
         true => false))

