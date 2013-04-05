(ns poky.protocol.http
  (:require 
    [poky.kv.core :as kv]
    (compojure [core :refer :all]
               [route :as route]
               [handler :as handler])
    [ring.util.response :refer [response not-found]]
    (ring.middleware [format-response :refer [wrap-restful-response]]
                     [format-params :as format-params])
    [cheshire.core :as json]
    [ring.middleware.stacktrace :as trace]))



(defn api
  [kvstore]
  (let [api-routes
        (routes
          (GET ["/:ks" :ks #"[\d\w-_.,]+"] {{:keys [ks]} :params}
               (let [eks (clojure.string/split ks #",")
                     nks (count eks)]
                 (if (> nks 1)
                   (response (kv/mget* kvstore eks))
                   (response (kv/get* kvstore ks))))))]
        (-> (handler/api api-routes)
          wrap-restful-response
          trace/wrap-stacktrace)))

