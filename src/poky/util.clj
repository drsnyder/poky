(ns poky.util
  (:import java.text.SimpleDateFormat
           java.util.TimeZone
           java.util.Locale))

(defn cmd-to-keyword [cmd]
  (if (keyword? cmd)
    cmd
    (keyword (clojure.string/lower-case cmd))))

<<<<<<< HEAD
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

=======
; inspiration came from
; https://github.com/ordnungswidrig/compojure-rest/blob/master/src/compojure_rest/util.clj
(defn http-date-format
  "Generate an HTTP date format."
  []
  (doto (new SimpleDateFormat "EEE, dd MMM yyyy HH:mm:ss" Locale/US)
    (.setTimeZone (TimeZone/getTimeZone "GMT"))))


(defn http-date
  "Convert date to an HTTP formatted date."
  [date]
  (->> date
       (.format (http-date-format))
       (format "%s GMT")))
>>>>>>> http-head
