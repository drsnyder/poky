(ns poky.util)

(defn cmd-to-keyword [cmd]
  (if (keyword? cmd)
    cmd
    (keyword (clojure.string/lower-case cmd))))

