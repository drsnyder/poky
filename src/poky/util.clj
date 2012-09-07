(ns poky.util)

(defn cmd-to-keyword [cmd]
  (keyword (clojure.string/lower-case cmd)))

