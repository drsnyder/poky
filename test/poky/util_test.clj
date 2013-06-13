(ns poky.util-test
  (:require [poky.util :as util]
            [environ.core :refer [env]]
            [midje.sweet :refer :all]))

(facts :first=
       (util/first= '(0) 1) => falsey
       (util/first= '(1) 1) => truthy
       (util/first= nil 1) => falsey)

(facts :quote-string
       (util/quote-string nil \") => nil
       (util/quote-string "s" nil) => nil
       (util/quote-string "s" \") => "\"s\"")

(facts :strip-char
       (util/strip-char nil nil) => nil
       (util/strip-char "str\"" nil) => nil
       (util/strip-char "str\"" \") => "str"
       (util/strip-char "\"str\"" \") => "str"
       (util/strip-char "\"s\"tr\"" \") => "str")
