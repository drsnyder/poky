(ns poky.protocol.http.jdbc.text
  (:require [poky.kv.core :as kv]
    (compojure [core :refer :all]
               [route :as route]
               [handler :as handler])
    [ring.util.response :refer [response not-found]]
    (ring.middleware [format-response :refer [wrap-restful-response]]
                     [format-params :as format-params])
    [cheshire.core :as json]
    [ring.middleware.stacktrace :as trace]))

;
; curl -d"some data" -H'Content-Type: application/text' -v -X PUT http://localhost:8080/xxx
; curl -d'{"bla":"bar"}' -H'Content-Type: application/json' -v -X PUT http://localhost:8080/bla

(def valid-key-regex #"[\d\w-_.,]+")

(defn- wrap-get
  [kvstore ks params headers body]
  (let [eks (clojure.string/split ks #",")
        nks (count eks)]
    (if (> nks 1)
      (response (kv/mget* kvstore eks))
      (response (kv/get* kvstore ks)))))


(defn- wrap-put
  [kvstore ks params headers body]
  (prn params) 
  (prn headers) 
  (prn body)
  (if (and 
        (= (get headers "content-type") "application/json")
        (get params (keyword ks) nil))
    (kv/set* kvstore ks (get params (keyword ks)))
    (kv/set* kvstore ks body))
  (response ""))

(defn api
  [kvstore]
  (let [api-routes
        (routes
          (GET ["/:ks" :ks valid-key-regex] {{:keys [ks] :as params} :params body :body headers :headers}
               (wrap-get kvstore ks params headers body))
          (PUT ["/:ks" :ks valid-key-regex] {{:keys [ks] :as params} :params 
                                             body :body body-params :body-params headers :headers} 
               (let [body (slurp body)
                     body (if (empty? body) body-params body)]
                 (wrap-put kvstore ks params headers body))))]

    (-> (handler/api api-routes)
        (format-params/wrap-format-params
          :predicate format-params/json-request?
          :decoder #(json/parse-string % true)
          :charset format-params/get-or-guess-charset)
        wrap-restful-response
        trace/wrap-stacktrace)))

