(ns poky.protocol.http.jdbc.text
  (:require [poky.kv.core :as kv]
            (compojure [core :refer :all]
                       [route :as route]
                       [handler :as handler])
            [ring.adapter.jetty :as jetty]
            [ring.util.response :refer [response not-found]]
            (ring.middleware [format-response :as format-response ]
                             [format-params :as format-params])
            [cheshire.core :as json]
            [environ.core :refer [env]]
            [ring.middleware.stacktrace :as trace]))

(def ^:private default-jetty-max-threads 25)
;
; curl -d"some data" -H'Content-Type: application/text' -v -X PUT http://localhost:8080/xxx
; curl -d'"some data"' -H'Content-Type: application/json' -v -X PUT http://localhost:8080/bla

(def valid-key-regex #"[\d\w-_.,]+")

(defn- wrap-get
  [kvstore k params headers body]
  (response (get (kv/get* kvstore k) k)))

(defn- wrap-put
  [kvstore k params headers body]
  (kv/set* kvstore k body)
  (response "")) ; empty 200

(defn api
  [kvstore]
  (let [api-routes
        (routes
          (GET ["/:k" :ks valid-key-regex] {{:keys [k] :as params} :params body :body headers :headers}
               (wrap-get kvstore k params headers body))
          (PUT ["/:k" :ks valid-key-regex] {{:keys [k] :as params} :params 
                                             body :body body-params :body-params headers :headers} 
               (let [body (slurp body)
                     body (if (empty? body) body-params body)]
                 (wrap-put kvstore k params headers body)))
          (POST ["/:k" :ks valid-key-regex] {{:keys [k] :as params} :params 
                                             body :body body-params :body-params headers :headers} 
               (let [body (slurp body)
                     body (if (empty? body) body-params body)]
                 (wrap-put kvstore k params headers body)))
          (route/not-found "Not Found"))]

    (-> (handler/api api-routes)
        (format-params/wrap-format-params
          :predicate format-params/json-request?
          :decoder #(json/parse-string % true)
          :charset format-params/get-or-guess-charset)
        ; for curl default content type & possibly others
        (format-params/wrap-format-params
          :predicate (format-params/make-type-request-pred #"^application/x-www-form-urlencoded")
          :decoder identity
          :charset format-params/get-or-guess-charset)
        (format-response/wrap-format-response
          :predicate format-response/serializable?
          :encoders [(format-response/make-encoder json/encode "application/json")
                     (format-response/make-encoder identity "text/plain")]
          :charset "utf-8")
        trace/wrap-stacktrace)))

(defn start-server
  "Start the jetty http server. 
  Environment:
    MAX_THREADS
  "
  [kvstore port]
  (jetty/run-jetty (api kvstore) 
                   {:port port
                    :max-threads (env :max-threads default-jetty-max-threads)
                    :join? false}))

    




