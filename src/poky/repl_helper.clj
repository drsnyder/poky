(ns poky.repl-helper
  (:use [clojure.repl :only (doc source)])
  (:require (poky.kv [jdbc :as kv.jdbc]
                     [core :refer :all])
            [poky.protocol.http :as http]
            [poky.protocol.memcache.codec :as memcache-codec]
            [poky.protocol.memcache :as memcache]
            [poky.system :as system]
            [poky.util :as util]
            [environ.core :refer [env]]
            [cheshire.core :as json]
            [midje.repl :refer :all]
            [clojure.java.jdbc :as sql])
  (:import java.nio.ByteBuffer))

(def S (delay (when-let [dsn (env :database-url)] (system/create-system (kv.jdbc/create dsn) #'http/start-server))))
(def M (delay (when-let [dsn (env :database-url)] (system/create-system (kv.jdbc/create dsn) #'memcache/start-server))))

; (system/start @S 8080)
; (system/stop @S)
; (system/start @M 11212)

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

