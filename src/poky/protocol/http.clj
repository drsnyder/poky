(ns poky.protocol.http
  (:use [clojure.data.json :only (read-json json-str)]
        [compojure.core]
        [ring.middleware json-params params])
  (:require [poky.core :as poky]
            [poky.db   :as db]
            [poky.vars :as pvars]))

(defn- json-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (json-str data)})


(defn do-add [k v] 
  (poky/add pvars/*connection* k v))

(defn do-gets [ks] 
  (poky/gets pvars/*connection* ks))



; curl 'http://localhost:3000/abc%2Ccde'
(defroutes http-handler
           (GET "/:keys" [keys]
                (json-response 
                  (do-gets (clojure.string/split keys #","))))

           (PUT "/" {params :params} 
                (json-response 
                  {"put" true :params params "key" (get "key" params) "value" (get "value" params)})))


(def http-app
  (-> http-handler
    wrap-params
    wrap-json-params))
