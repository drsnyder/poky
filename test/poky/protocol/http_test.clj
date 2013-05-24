(ns poky.protocol.http-test
  (:require [poky.protocol.http :as http]
            [poky.kv.core :as kv]
            [poky.util :as util]
            [environ.core :refer [env]]
            [midje.util :refer [expose-testables]]
            [midje.sweet :refer :all])
  (:import [java.util.DateFormat]))

; not working
;(expose-testables poky.protocol.http)

(facts :get
       (#'http/wrap-get ..store.. ..bucket.. "some-key" 
                        ..params.. ..headers.. ..body..) => (contains {:body "some-value"
                                                                       :headers map?
                                                                       :status 200})
       (provided
         (kv/get* ..store.. ..bucket.. "some-key") => {"some-key" "some-value" :modified_at (java.util.Date.)})

       (#'http/wrap-get ..store.. ..bucket.. ..key.. 
                        ..params.. ..headers.. ..body..) => (contains {:body ""
                                                                       :headers map?
                                                                       :status 404})
       (provided
         (kv/get* ..store.. ..bucket.. ..key..) => nil))


(facts :put :post
       (#'http/wrap-put ..store.. ..bucket.. ..key..
                        ..params.. ..headers.. ..body..) => (contains {:body ""
                                                                       :headers map?
                                                                       :status 200})
       (provided
         (kv/set* ..store.. ..bucket.. ..key.. ..body..) => :updated)

       (#'http/wrap-put ..store.. ..bucket.. ..key..
                        ..params.. ..headers.. ..body..) => (contains {:body ""
                                                                       :headers map?
                                                                       :status 200})
       (provided
         (kv/set* ..store.. ..bucket.. ..key.. ..body..) => :inserted)

       (#'http/wrap-put ..store.. ..bucket.. ..key..
                        ..params.. ..headers.. ..body..) => (contains {:body ""
                                                                       :headers map?
                                                                       :status 412})
       (provided
         (kv/set* ..store.. ..bucket.. ..key.. ..body..) => :rejected))


(facts :delete
       (#'http/wrap-delete ..store.. ..bucket.. ..key..
                        ..params.. ..headers..) => (contains {:body ""
                                                              :headers map?
                                                              :status 200})
       (provided
         (kv/delete* ..store.. ..bucket.. ..key..) => true)

       (#'http/wrap-delete ..store.. ..bucket.. ..key..
                        ..params.. ..headers..) => (contains {:body ""
                                                              :headers map?
                                                              :status 404})
       (provided
         (kv/delete* ..store.. ..bucket.. ..key..) => false))
