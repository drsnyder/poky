(ns poky.util)

(defn cmd-to-keyword [cmd]
  (if (keyword? cmd)
    cmd
    (keyword (clojure.string/lower-case cmd))))

(defn- random-char-set
  ([]
   (shuffle (map (comp char (partial + 65)) (range 0 58))))
  ([n]
   (take n (random-char-set))))

(defn random-string
  ([]
   (clojure.string/join "" (random-char-set)))
  ([n]
   (clojure.string/join "" (random-char-set n))))

