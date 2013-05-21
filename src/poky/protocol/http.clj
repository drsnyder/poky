(ns poky.protocol.http
  (:require [poky.kv.core :as kv]
            (compojure [core :refer :all]
                       [route :as route]
                       [handler :as handler])
            [ring.adapter.jetty :as jetty]
            [ring.util.response :refer [response not-found charset]]
            (ring.middleware [format-response :as format-response ]
                             [format-params :as format-params])
            [cheshire.core :as json]
            [environ.core :refer [env]]
            [ring.middleware.stacktrace :as trace]))

(def ^:private default-jetty-max-threads 25)

(def valid-key-regex #"[\d\w-_.,]+")

(defn- wrap-get
  [kvstore b k params headers body]
  (if-let [v (get (kv/get* kvstore b k) k)]
    (response v)
    (not-found "")))

(defn- wrap-put
  [kvstore b k params headers body]
  (kv/set* kvstore b k body)
  (response "")) ; empty 200

(defn- wrap-delete
  [kvstore b k params headers]
  (if (kv/delete* kvstore b k)
    (response "")    ; empty 200
    (not-found ""))) ; empty 404



(defn- put-body
  "Returns body if non-empty otherwise body-params"
  [body body-params]
  (let [body (slurp body)]
    (if (clojure.string/blank? body) body-params body)))

(defn- wrap-charset
  "Middleware for setting the charset of the response."
  [handler char-set]
  (fn [req]
    (charset (handler req) char-set)))

(defn kv-routes
  [kvstore]
  (let [kv-api-routes
        (routes
          (GET ["/:b/:k" :b valid-key-regex :k valid-key-regex]
               {:keys [params headers body] {:keys [b k]} :params}
               (wrap-get kvstore b k params headers body))
          (PUT ["/:b/:k" :b valid-key-regex :k valid-key-regex]
               {:keys [params body body-params headers] {:keys [b k]} :params}
               (wrap-put kvstore b k params headers (put-body body body-params)))
          (POST ["/:b/:k" :b valid-key-regex :k valid-key-regex]
                {:keys [params body body-params headers] {:keys [b k]} :params}
                (wrap-put kvstore b k params headers (put-body body body-params)))
          (DELETE ["/:b/:k" :b valid-key-regex :k valid-key-regex]
                {:keys [params body body-params headers] {:keys [b k]} :params}
                (wrap-delete kvstore b k params headers))
          (route/not-found "Object not found. Did you specifiy /:bucket/:key?"))]
    kv-api-routes))


(defroutes status-routes 
  (GET "/" []
       (response "ok")))

(defn api
  [kvstore]
  (let [api-routes (routes 
                     (context "/kv" [] (kv-routes kvstore))
                     (context "/status" [] status-routes))]
    (-> (handler/api api-routes)
        ; for curl default content type & possibly others. pass the data
        ; through as is
        (format-params/wrap-format-params
          :predicate (format-params/make-type-request-pred #"^application/x-www-form-urlencoded")
          :decoder identity
          :charset "utf-8")
        ; this is required to make sure we handle multi-byte content responses
        ; properly
        (wrap-charset "utf-8")
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