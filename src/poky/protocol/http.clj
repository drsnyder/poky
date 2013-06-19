(ns poky.protocol.http
  (:require [poky.kv.core :as kv]
            [poky.util :as util]
            [clojure.tools.logging :refer [infof warnf]]
            (compojure [core :refer :all]
                       [route :as route]
                       [handler :as handler])
            [ring.adapter.jetty :as jetty]
            [ring.util.response :refer [response status not-found charset header]]
            (ring.middleware [format-response :as format-response ]
                             [format-params :as format-params])
            [cheshire.core :as json]
            [environ.core :refer [env]]
            [clj-time.coerce :as tc]
            [ring.middleware.stacktrace :as trace]
            [ring.middleware.statsd :as statsd]))

(def ^:private default-jetty-max-threads 25)

(def ^:private help-message "
For key-value objects, the following are supported:
  - PUT    /kv/:bucket/:key | creates object
  - POST   /kv/:bucket/:key | creates object
  - DELETE /kv/:bucket/:key | deletes object
  - GET    /kv/:bucket/:key | returns object

Other:
  - GET /status | returns 'ok' and status 200

Status codes to expect:
  - 200 on success
  - 404 when the object is not found
  - 412 when an update is rejected
  - 500 on server error
")

(def valid-key-regex #"[\d\w-_.,]+")

(defn add-response-header
  "Add a header field and value to the response object r."
  [r field value]
  (cond-> r
          value (header field value)))

(defn generate-etag
  "Generate an etag given a string."
  [^:String s]
  (when s
    s))

(defn- wrap-get
  [kvstore b k params headers body]
  (let [if-match (util/strip-char (or (get headers "if-match") (get headers "x-if-match")) \")]
    (if-let [t (kv/get* kvstore b k)]
      (let [modified (util/Timestamp->http-date (get t :modified_at nil))
            etag (generate-etag modified)]
        (if (or (not if-match) (= if-match etag) (= if-match "*"))
          (-> (response (get t k))
            (add-response-header "Last-Modified" modified)
            (add-response-header "ETag" (util/quote-string etag \")))
          (do
            (warnf "GET rejected for '%s/%s' If-Match (%s) != etag (%s)" b k if-match etag)
            (-> (response "") (status 412)))))
      (if (and if-match (= if-match "*"))
        (-> (response "") (status 412))
        (not-found "")))))

(defn- wrap-put
  [kvstore b k params headers body]
  (let [if-unmodified-since (get headers "if-unmodified-since" nil)
        modified (util/http-date->Timestamp if-unmodified-since)]
    (if-not if-unmodified-since
      (warnf "If-Unmodified-Since not provided for %s/%s" b k))
    (if (and if-unmodified-since (not modified))
      ; if If-Unmodified-Since was specified in the header, but didn't parse,
      ; reject this as a bad request.
      (-> (response "Error in If-Unmodified-Since format. Use RFC 1123 date format.")
          (status 400))
      (condp = (kv/set* kvstore b k body {:modified modified})
        :updated (response "")
        :inserted (response "")
        :rejected (do
                    (warnf "PUT/POST rejected for '%s/%s'" b k)
                    (-> (response "") (status 412)))
        (-> (response "Error, PUT/POST could not be completed.") (status 500))))))

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
          (GET "/help" []
               (response help-message))
          (route/not-found (str "Object not found.\n" help-message)))]
    kv-api-routes))

(defroutes fall-back-routes
  (ANY "*" []
       (route/not-found help-message)))

(defroutes status-routes
  (GET "/" []
       (response "ok")))

(defn api
  [kvstore]
  (let [api-routes (routes
                     (context "/kv" [] (kv-routes kvstore))
                     (context "/status" [] status-routes)
                     (context "*" [] fall-back-routes))]
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
        trace/wrap-stacktrace
        (statsd/wrap-request-method-counter (str (env :statsd-key-base  "poky") ".req_method"))
        (statsd/wrap-response-code-counter (str (env :statsd-key-base  "poky") ".resp_status")))))


(defn start-server
  "Start the jetty http server.
  Environment:
  MAX_THREADS
  STATSD_HOST
  STATSD_KEY_BASE
  "
  [kvstore port]
  (when-let [statsd-host (env :statsd-host)]
    (let [[host port] (clojure.string/split statsd-host #":")]
      (when (and host port)
        (infof "Sending statsd metrics to %s" statsd-host)
        (statsd/setup! host (Integer/parseInt port)))))

  (infof "Starting poky on port %d" port)
  (jetty/run-jetty (api kvstore)
                   {:port port
                    :max-threads (util/parse-int (env :max-threads default-jetty-max-threads))
                    :join? false}))
