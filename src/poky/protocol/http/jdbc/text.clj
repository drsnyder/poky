(ns poky.protocol.http.jdbc.text
  (:require [poky.kv.core :as kv]
    (compojure [core :refer :all]
               [route :as route]
               [handler :as handler])
    [ring.util.response :refer [response not-found]]
    (ring.middleware [format-response :as format-response ]
                     [format-params :as format-params])
    [cheshire.core :as json]
    [ring.middleware.stacktrace :as trace]))

;
; curl -d"some data" -H'Content-Type: application/text' -v -X PUT http://localhost:8080/xxx
; curl -d'{"bla":"bar"}' -H'Content-Type: application/json' -v -X PUT http://localhost:8080/bla

(def valid-key-regex #"[\d\w-_.,]+")

; FIXME: this should be split- one fn for get, one for mget
(defn- wrap-get
  [kvstore ks params headers body]
  (response 
    (let [eks (clojure.string/split ks #",")
          nks (count eks)
          multi (> nks 1)
          ret (if multi (kv/mget* kvstore eks) (kv/get* kvstore ks))]
    (condp = (get headers "accept")
      "application/json" ret
      "text/plain" (if multi (throw (Exception. "Multi get unsupported with Accept: text/plain")) (get ret ks))
      ret))))



(defn- wrap-put
  [kvstore ks params headers body]
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
        (format-response/wrap-format-response
          :predicate format-response/serializable?
          :encoders [(format-response/make-encoder json/encode "application/json")
                     (format-response/make-encoder identity "text/plain")]
          :charset "utf-8")
        trace/wrap-stacktrace)))

