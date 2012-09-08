(ns poky.repl-helper
  (:use [clojure.java.jdbc :as sql :only [with-connection]]
        [clojure pprint walk]
        [poky db util]
        [poky.protocol memcache http]
        [poky.server memcache]
        [lamina.core]
        [aleph.tcp]
        [gloss core io])
  (:require [poky.core :as poky])
  (:import java.nio.ByteBuffer)
  (:import [java.lang.reflect Method]))


(declare buffer-to-string)
(defn frame-to-string [f]
  (apply str (concat 
               (postwalk #(if (instance? java.nio.ByteBuffer %) 
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
