(ns poky.util
  (:import java.text.SimpleDateFormat
           java.util.TimeZone
           java.util.Locale))

(defn cmd-to-keyword [cmd]
  (if (keyword? cmd)
    cmd
    (keyword (clojure.string/lower-case cmd))))

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
