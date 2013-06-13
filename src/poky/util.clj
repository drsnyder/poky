(ns poky.util
  (:require (clj-time [coerce :as tc]
                      [format :as tf]))
  (:import java.sql.Timestamp
           java.text.SimpleDateFormat
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


(def rfc1123 "EEE, dd MMM yyyy HH:mm:ss 'GMT'")
(def rfc1123-format (tf/formatter rfc1123))

(defn Timestamp->http-date
  "Convert java.sql.Timestamp to an HTTP formatted date."
  [#^Timestamp date]
  (when date
    (try
      (tf/unparse rfc1123-format (tc/from-sql-date date))
      ; TODO: how should we handle this?
      (catch java.lang.IllegalArgumentException e
        nil))))

(defn http-date->Timestamp
  "Convert an RFC1123 date string to a java.sql.Timestamp."
  [#^String date]
  (when date
    (try
      (tc/to-timestamp (tf/parse rfc1123-format date))
      (catch java.lang.IllegalArgumentException e
        ; TODO: how should we handle this?
        ; this is probably exceptional since if we return nil, it will cause an
        ; unconditional write
        nil))))



(defn first=
  "Compare the first value in s to v using =. Complements set and delete.
  The clojure.java.jdbc methods they use return a tuple where the first element is the
  number of records updated. This helper can be used to test that element for the number
  expected."
  [coll v]
  (when-first [a coll] (= a v)))


(defn quote-string
  "Quote a string with the character(s) c."
  [^:String s c]
  (when (and s c)
    (str c s c)))

(defn strip-char
  "Strip the char c from s."
  [s c]
  (when (and s c)
    (clojure.string/replace s (re-pattern (str c)) "")))
