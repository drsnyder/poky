(ns poky.util
  (:import java.text.SimpleDateFormat
           java.util.TimeZone
           java.util.Locale))


(def ascii-table-base 65)
(def ascii-table-range 58)

(defn random-ascii-block
  "Generate a random set of characters from an ASCII block. If base and table-range
  are omitted a set of characters that is safe for printing is used."
  ([base table-range]
   (shuffle (map (comp char (partial + base)) (range 0 table-range))))
  ([]
   (random-ascii-block ascii-table-base ascii-table-range)))


(defn random-char-set
  "Generate a random seq of printable ASCII characters. If n is supplied, return a list
  of n characters. If n exceeds ~58 there will be repititions."
  ([]
   (cons (nth (random-ascii-block) (rand-int ascii-table-range)) (lazy-seq (random-char-set))))
  ([n]
   (take n (random-char-set))))

(defn random-string
  ([]
   (clojure.string/join "" (random-char-set)))
  ([n]
   (clojure.string/join "" (random-char-set n))))

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


(defn first=
  "Compare the first value in s to v using =. Complements set and delete.
  The clojure.java.jdbc methods they use return a tuple where the first element is the
  number of records updated. This helper can be used to test that element for the number
  expected."
  [coll v]
  (when-first [a coll] (= a v)))
