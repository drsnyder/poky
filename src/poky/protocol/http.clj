(ns poky.protocol.http
  (:require 
    [poky.kv.core :as kv]
    (compojure [core :refer :all]
               [route :as route]
               [handler :as handler])
    [ring.util.response :refer [response not-found]]
    (ring.middleware [format-response :refer [wrap-restful-response]]
                     [format-params :refer [wrap-json-params]])
    [cheshire.core :as json]
    [ring.middleware.stacktrace :as trace]))

(def valid-key-regex #"[\d\w-_.,]+")

(defn api
  [kvstore]
  (let [api-routes
        (routes
          (GET ["/:ks" :ks valid-key-regex] {{:keys [ks]} :params}
               (let [eks (clojure.string/split ks #",")
                     nks (count eks)]
                 (if (> nks 1)
                   (response (kv/mget* kvstore eks))
                   (response (kv/get* kvstore ks)))))
          (PUT ["/:ks" :ks valid-key-regex] {{:keys [ks] :as params} :params} 
               (prn params)))]
    (-> (handler/api api-routes)
        wrap-json-params
        wrap-restful-response
        trace/wrap-stacktrace)))

