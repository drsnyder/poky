(ns poky.repl-helper
  (:require 
    (poky.kv [memory :as m]
             [core :refer :all])
    [poky.kv.jdbc.util :as jdbc-util]
    [poky.kv.jdbc.text :as text]
    [poky.protocol.http.jdbc.text :as http]
    [poky.protocol.memcache.jdbc.text :as memcache]
    [poky.system :as system]
    [environ.core :refer [env]]
    [cheshire.core :as json]
    [midje.repl :refer :all]
    [clojure.java.jdbc :as sql])
  (:import java.nio.ByteBuffer))

(def S (system/create-system (text/create (env :database-url)) #'http/start-server))
(def M (system/create-system (text/create (env :database-url)) #'memcache/start-server))

; (system/start S 8080)
; (system/stop S)
; (system/start M 11212)

(declare buffer-to-string)
(defn frame-to-string [f]
  (apply str (concat 
               (clojure.walk/postwalk #(if (instance? java.nio.ByteBuffer %) 
                       (buffer-to-string %)
                       %) f))))

(defn buffer-to-string [b]
  (let [cap (.capacity b)]
    (if (> cap 0)
      (loop [len (.capacity b)
             result ()]
        (if (= 0 len)
          (apply str (map #(str (char %)) result))
          (recur (dec len) (conj result (.get b (dec len))))))
      nil)))

