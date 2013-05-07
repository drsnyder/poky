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
            [ring.middleware.stacktrace :as trace]
            [clojure.string :as string]))

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

(defn- put-body
  "Returns body if non-empty otherwise body-params"
  [body body-params]
  (let [body (slurp body)]
    (if (clojure.string/blank? body) body-params body)))

(defn api
  [kvstore]
  (let [api-routes
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
          (route/not-found ""))]

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
