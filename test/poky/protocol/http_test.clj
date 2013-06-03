(ns poky.protocol.http-test
  (:require [poky.protocol.http :as http]
            [poky.kv.core :as kv]
            [poky.kv.jdbc :as kv.jdbc]
            [poky.kv.jdbc.util :refer :all]
            [poky.util :as util]
            [poky.system :as system]
            [environ.core :refer [env]]
            [clj-http.client :as client]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [midje.util :refer [expose-testables]]
            [midje.sweet :refer :all])
  (:import [java.util.DateFormat]))

; not working
;(expose-testables poky.protocol.http)



(def bucket (str (.name *ns*)))
(def S (atom nil))
(def default-port 9999)

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
         (kv/set* ..store.. ..bucket.. ..key.. ..body..) => :rejected)

       (#'http/wrap-put ..store.. ..bucket.. ..key..
                        ..params.. ..headers.. ..body..) => (contains {:body "Error, PUT/POST could not be completed."
                                                                       :headers map?
                                                                       :status 500})
       (provided
         (kv/set* ..store.. ..bucket.. ..key.. ..body..) => false))


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

(defn create-system
 []
  (when-let [dsn (env :database-url)] (system/create-system (kv.jdbc/create dsn) #'http/start-server)))

(defn end-point-url
  [port b k]
  (format "http://localhost:%d/kv/%s/%s", port, bucket, k))

(with-state-changes [(around :facts (do (reset! S (create-system))
                                        (purge-bucket (kv/connection (system/store @S)) bucket)
                                        (system/start @S default-port)
                                        ?form
                                        (system/stop @S)
                                        (kv/close (system/store @S))))]

  (facts :integration :put
         (client/put (end-point-url default-port bucket "set-me")
                      {:headers {"if-unmodified-since" (tf/unparse (tf/formatters :rfc822) (t/now))}
                       :body "with-a-value"}) => (contains {:status 200})
         ; If-Unmodified-Since in the past, should be rejected
         (client/put (end-point-url default-port bucket "set-me")
                      {:throw-exceptions false
                       :headers {"if-unmodified-since" (tf/unparse (tf/formatters :rfc822) (t/minus (t/now) (t/days 1)))}
                       :body "with-a-value"}) => (contains {:status 412})
         (client/get (end-point-url default-port bucket "set-me")) => (contains {:status 200
                                                                                 :body "with-a-value"}))

  (facts :integration :put :multi-byte
         (client/put (end-point-url default-port bucket "put-multi-byte")
                      {:body "讓我們吃的點心"}) => (contains {:status 200})
         (client/get (end-point-url default-port bucket "put-multi-byte")) => (contains {:status 200
                                                                                     :body "讓我們吃的點心"}))
  (facts :integration :post
         (client/post (end-point-url default-port bucket "set-me")
                      {:body "with-a-value"}) => (contains {:status 200})
         (client/get (end-point-url default-port bucket "set-me")) => (contains {:status 200
                                                                                 :body "with-a-value"}))

  (facts :integration :post :multi-byte
         (client/post (end-point-url default-port bucket "post-multi-byte")
                      {:body "你想去哪兒吃"}) => (contains {:status 200})
         (client/get (end-point-url default-port bucket "post-multi-byte")) => (contains {:status 200
                                                                                     :body "你想去哪兒吃"}))


  (facts :integration :delete
         (client/delete (end-point-url default-port bucket "delete-me")
                        {:throw-exceptions false}) => (contains {:status 404})
         (client/post (end-point-url default-port bucket "delete-me")
                      {:body "with-a-value"}) => (contains {:status 200})
         (client/delete (end-point-url default-port bucket "delete-me") 
                        {:throw-exceptions false}) => (contains {:status 200})
         (client/delete (end-point-url default-port bucket "delete-me")
                        {:throw-exceptions false}) => (contains {:status 404})))
