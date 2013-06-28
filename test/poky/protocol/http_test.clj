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
            [clj-time.coerce :as tc]
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
           ..headers.. ..body..) => (contains {:body "some-value"
                                                          :headers map?
                                                          :status 200})
       (provided
         (kv/get* ..store.. ..bucket.. "some-key") => {"some-key" "some-value" :modified_at (java.util.Date.)})

       (#'http/wrap-get ..store.. ..bucket.. ..key..
           ..headers.. ..body..) => (contains {:body ""
                                                          :headers map?
                                                          :status 404})
       (provided
         (kv/get* ..store.. ..bucket.. ..key..) => nil)


       (#'http/wrap-get ..store.. ..bucket.. ..key..
           {"if-match" "*"} ..body..) => (contains {:body ""
                                                               :headers map?
                                                               :status 412})
       (provided
         (kv/get* ..store.. ..bucket.. ..key..) => nil)

       (let [now (t/now)
             etag (http/generate-etag (tf/unparse util/rfc1123-format now))
             later (t/plus now (t/days 1))]
         (#'http/wrap-get ..store.. ..bucket.. "some-key"
             {"if-match" etag} ..body..) => (contains {:body "some-value"
                                                                  :headers map?
                                                                  :status 200})
         (provided
           (kv/get* ..store.. ..bucket.. "some-key") => {"some-key" "some-value" :modified_at (tc/to-timestamp now)})

         (#'http/wrap-get ..store.. ..bucket.. "some-key"
             {"if-match" etag} ..body..) => (contains {:body ""
                                                                  :headers map?
                                                                  :status 412})
         (provided
           (kv/get* ..store.. ..bucket.. "some-key") => {"some-key" "some-value" :modified_at (tc/to-timestamp later)})))



(facts :put :post
       (#'http/wrap-put ..store.. ..bucket.. ..key..
                        ..headers.. ..body.. ..uri..) => (contains {:body ""
                                                                       :headers map?
                                                                       :status 200})
       (provided
         (kv/set* ..store.. ..bucket.. ..key.. ..body.. {:modified_at nil}) => :updated)

       (#'http/wrap-put ..store.. ..bucket.. ..key..
                        ..headers.. ..body.. ..uri..) => (contains {:body ""
                                                                       :headers map?
                                                                       :status 200})
       (provided
         (kv/set* ..store.. ..bucket.. ..key.. ..body.. {:modified_at nil}) => :inserted)

       (#'http/wrap-put ..store.. ..bucket.. ..key..
                        ..headers.. ..body.. ..uri..) => (contains {:body ""
                                                                       :headers map?
                                                                       :status 412})
       (provided
         (kv/set* ..store.. ..bucket.. ..key.. ..body.. {:modified_at nil}) => :rejected)

       (#'http/wrap-put ..store.. ..bucket.. ..key..
                        ..headers.. ..body.. ..uri..) => (contains {:body "Error, PUT/POST could not be completed."
                                                                       :headers map?
                                                                       :status 500})
       (provided
         (kv/set* ..store.. ..bucket.. ..key.. ..body.. {:modified_at nil}) => false)

       (#'http/wrap-put ..store.. ..bucket.. ..key..
                        {"if-unmodified-since" "bogus"} ..body.. ...uri...) => (contains
                                                                                    {:body "Error in If-Unmodified-Since format. Use RFC 1123 date format."
                                                                                     :headers map?
                                                                                     :status 400}))


(facts :delete
       (#'http/wrap-delete ..store.. ..bucket.. ..key..
                        ..headers.. ..uri..) => (contains {:body ""
                                                              :headers map?
                                                              :status 200})
       (provided
         (kv/delete* ..store.. ..bucket.. ..key..) => true)

       (#'http/wrap-delete ..store.. ..bucket.. ..key..
                        ..headers.. ..uri..) => (contains {:body ""
                                                              :headers map?
                                                              :status 404})
       (provided
         (kv/delete* ..store.. ..bucket.. ..key..) => false))

(defn create-system
 []
  (when-let [dsn (env :database-url)] (system/create-system (kv.jdbc/create dsn) #'http/start-server)))

(defn end-point-url
  [port b k &{:keys [host] :or {host "localhost"}}]
  (format "http://%s:%d/kv/%s/%s", host port, bucket, k))


(with-state-changes
  [(around :facts (do (reset! S (create-system))
                    (purge-bucket (kv/connection (system/store @S)) bucket)
                    (system/start @S default-port)
                    ?form
                    (system/stop @S)
                    (kv/close (system/store @S))))]

  (facts :integration :get
         (client/get
           (end-point-url default-port bucket "set-me")
           {:throw-exceptions false
            :headers {"If-Match" "*"}}) => (contains {:status 412
                                                      :body ""})

         (client/get
           (end-point-url default-port bucket "set-me")
           {:throw-exceptions false}) => (contains {:status 404
                                                    :body ""})

         (let [now (t/now)
               ts (tf/unparse util/rfc1123-format now)
               etag (http/generate-etag ts)
               later (t/plus now (t/days 1))
               lateretag (http/generate-etag (tf/unparse util/rfc1123-format later))]
           (client/put
             (end-point-url default-port bucket "set-me")
             {:headers {"if-unmodified-since" ts}
              :body "with-a-value"}) => (contains {:status 200})

         (client/get
           (end-point-url default-port bucket "set-me")
           {:headers {"If-Match" etag}}) => (contains {:status 200
                                                       :headers #(string? (get % "etag"))
                                                       :body "with-a-value"})

         (client/get
           (end-point-url default-port bucket "set-me")) => (contains {:status 200
                                                                       :headers #(string? (get % "etag"))
                                                                       :body "with-a-value"})

         (client/get
           (end-point-url default-port bucket "set-me")
           {:throw-exceptions false
            :headers {"If-Match" lateretag}}) => (contains {:status 412
                                                            :body ""})))

  (facts :integration :put
         (client/put
           (end-point-url default-port bucket "set-me")
           {:headers {"if-unmodified-since" (tf/unparse util/rfc1123-format (t/now))}
            :body "with-a-value"}) => (contains {:status 200})

         (client/get
           (end-point-url default-port bucket "set-me")) => (contains {:status 200
                                                                       :headers #(string? (get % "last-modified"))
                                                                       :body "with-a-value"})

         ; If-Unmodified-Since in the past, should be rejected
         (client/put
           (end-point-url default-port bucket "set-me")
           {:throw-exceptions false
            :headers {"if-unmodified-since"
                      (tf/unparse util/rfc1123-format (t/minus (t/now) (t/days 1)))}
            :body "with-a-value"}) => (contains {:status 412})

         ; Malformed If-Unmodified-Since should be rejected
         (client/put
           (end-point-url default-port bucket "set-me")
           {:throw-exceptions false
            :headers {"if-unmodified-since" "Tue, 04 Jun 2013 03:01:31 000"}
            :body "with-a-value"}) => (contains {:status 400 :body string?})

         ; PUT without if-unmodified-since should be accepted
         (client/put
           (end-point-url default-port bucket "set-me")
           {:throw-exceptions false
            :body "NEW-with-a-value"}) => (contains {:status 200})

         (client/get
           (end-point-url default-port bucket "set-me")) => (contains {:status 200
                                                                       :headers #(string? (get % "last-modified"))
                                                                       :body "NEW-with-a-value"}))

  (facts :integration :put :multi-byte
         (client/put
           (end-point-url default-port bucket "put-multi-byte")
           {:body "讓我們吃的點心"}) => (contains {:status 200})
         (client/get
           (end-point-url default-port bucket "put-multi-byte")) => (contains
                                                                      {:status 200
                                                                       :body "讓我們吃的點心"}))
  (facts :integration :post
         (client/post
           (end-point-url default-port bucket "set-me")
           {:body "with-a-value"}) => (contains {:status 200})
         (client/get
           (end-point-url default-port bucket "set-me")) => (contains {:status 200
                                                                       :body "with-a-value"}))

  (facts :integration :post :multi-byte
         (client/post
           (end-point-url default-port bucket "post-multi-byte")
           {:body "你想去哪兒吃"}) => (contains {:status 200})
         (client/get
           (end-point-url default-port bucket "post-multi-byte")) => (contains
                                                                       {:status 200
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


(defn purge
  [host port b k]
  (util/varnish-purge host port (format "/kv/%s/%s" b k)))

(facts :integration :varnish :get
       (if (and (env :varnish-integration-host) (env :varnish-integration-port))
         (let [varnish-host (env :varnish-integration-host)
               varnish-port (util/parse-int (env :varnish-integration-port))
               now (t/now)
               http-date (tf/unparse util/rfc1123-format (t/now))
               http-date-future (tf/unparse util/rfc1123-format (t/plus now (t/days 1)))
               test-uri (end-point-url varnish-port bucket "set-me" :host varnish-host)]
           (prn test-uri)

           (client/delete test-uri {:throw-exceptions false}) 

           (client/get test-uri {:throw-exceptions false}) => (contains {:status 404})

           (client/post test-uri {:body "with-a-value"
                                  :headers {"if-unmodified-since" http-date}}) => (contains {:status 200})

           ;; no If-Match; both of these should delete below
           (client/get test-uri) => (contains {:status 200
                                               :headers #(= (get % "x-cache") "MISS")
                                               :body "with-a-value"})
           (client/get test-uri) => (contains {:status 200
                                               :headers #(= (get % "x-cache") "HIT")
                                               :body "with-a-value"})

           (client/get test-uri {:headers {"If-Match" http-date}}) => (contains {:status 200 
                                                                                 :headers #(= (get % "x-cache") "MISS")
                                                                                 :body "with-a-value"})
           (client/get test-uri {:headers {"If-Match" http-date}}) => (contains {:status 200
                                                                                 :headers #(= (get % "x-cache") "HIT")
                                                                                 :body "with-a-value"})

           ;; make an edit and repeat
           (client/post test-uri {:body "with-a-value-edit"
                                  :headers {"if-unmodified-since" http-date-future}}) => (contains {:status 200})

           (client/get test-uri) => (contains {:status 200
                                               :headers #(= (get % "x-cache") "MISS")
                                               :body "with-a-value-edit"})
           (client/get test-uri) => (contains {:status 200
                                               :headers #(= (get % "x-cache") "HIT")
                                               :body "with-a-value-edit"})

           (client/get test-uri {:headers {"If-Match" http-date-future}}) => (contains {:status 200
                                                                                 :headers #(= (get % "x-cache") "MISS")
                                                                                 :body "with-a-value-edit"})
           (client/get test-uri {:headers {"If-Match" http-date-future}}) => (contains {:status 200
                                                                                 :headers #(= (get % "x-cache") "HIT")
                                                                                 :body "with-a-value-edit"})


           (client/delete test-uri {:throw-exceptions false}) => (contains {:status 200})
           (client/get test-uri {:throw-exceptions false}) => (contains {:status 404})
           (client/get test-uri {:throw-exceptions false :headers {"If-Match" http-date}}) => (contains {:status 404})


           (dorun
             (for [i (range 0 100)
                   :let [iuri (end-point-url varnish-port bucket (format "set-me-%d" i) :host varnish-host)]]
               (do
                 (client/delete iuri {:throw-exceptions false})
                 (client/get iuri {:throw-exceptions false}) => (contains {:status 404})
                 (client/post iuri {:body (str "with-a-value-" i)
                                    :headers {"if-unmodified-since" http-date}}) => (contains {:status 200})

                 (client/get iuri {:throw-exceptions false}) => (contains {:status 200
                                                                           :headers #(= (get % "x-cache") "MISS")})
                 (client/get iuri {:throw-exceptions false}) => (contains {:status 200
                                                                           :headers #(= (get % "x-cache") "HIT")})

                 (client/get iuri {:headers {"If-Match" http-date}}) => (contains {:status 200
                                                                                   :headers #(= (get % "x-cache") "MISS")
                                                                                   :body (str "with-a-value-" i)})
                 (client/get iuri {:headers {"If-Match" http-date}}) => (contains {:status 200
                                                                                   :headers #(= (get % "x-cache") "HIT")
                                                                                   :body (str "with-a-value-" i)})

                 ; both forms should be purged on delete
                 (client/delete iuri {:throw-exceptions false}) => (contains {:status 200})
                 (client/get iuri {:throw-exceptions false}) => (contains {:status 404})
                 (client/get iuri {:throw-exceptions false :headers {"If-Match" http-date}}) => (contains {:status 404}))))
           )

         (do
           (println "**** Set VARNISH_PURGE_HOST and VARNISH_PURGE_PORT in the environment. ****")
           (println "You will also need varnish and poky running as a service."))))
