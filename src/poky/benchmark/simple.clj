(ns poky.benchmark.simple
  (:require [poky.kv.core :as kv]
            [poky.kv.jdbc.util :as jdbc-util]
            [poky.util :as util]
            [clojure.tools.logging :refer [infof warnf]]
            [clj-logging-config.log4j :refer [set-logger!]]
            [environ.core :refer [env]]
            (clj-time [coerce :as tc]
                      [format :as tf])))

(def pg-default-time-fmt "yyyy-MM-dd HH:mm:ss+00")
(def pg-default-date (tf/formatter pg-default-time-fmt))

(defn csv-time->timestamp
  "Convert a timestamp in it's default format to something that JDBC can use for comparison to a timestamp."
  [str-ts]
  (tc/to-timestamp (tf/parse pg-default-date str-ts)))

(defn load-lookup-set
  "Load the rows from the given file hashed by the first column which should be the bucket. The expected format is:

  bucket,key,modified_at

  The modified_at should be rounded to the second.
  "
  [m file]
  (let [lines (util/read-file file)
        csv (map #(clojure.string/split % #",") lines)]
    (reduce (fn [init row]
              (update-in init
                         [(first row)]
                         conj {:key (second row) :modified_at (csv-time->timestamp (last row)) }))
            m csv)))

(defn load-lookup-sets
  "Loads all of the files into an empty map"
  ([m files]
   (reduce load-lookup-set m files))
  ([files]
   (load-lookup-sets {} files)))

(defn create-mget-bench-input
  "Create mget benchmark input. This function takes a loaded map from (load-lookup-sets) and creates tupels of
  of the form

  (bucket {:key \"k1\" :modified_at \"ts\"} ...)

  with request-size maps for input into jdbc-mget.
  "
  [m request-size]
  (let [by-bucket (map (fn [bucket]
                         ; split the k,v pairs up into the request size blocks
                         (let [blocks (partition request-size (get m bucket)) ]
                           ; add the bucket to the front of the block to identify it
                           (map #(conj % bucket) blocks)))
                       (keys m))]
    (shuffle (apply concat by-bucket))))


(defn current-time
  []
  (. java.lang.System  (clojure.core/nanoTime)))

(defn elapsed-time
  ([start] (elapsed-time start (current-time)))
  ([start stop]
   (/ (double (- stop start))
      1000000.0)))

(comment
  (bench/bench-jdbc-mget
    (connection (system/store S))
    ["tmp/mothering.csv" "tmp/basenotes.csv" "tmp/avsforum.csv" "tmp/headfi.csv"]
    30))

(defn bench-jdbc-mget
  "Benchmark jdbc-mget using the given list of input files and a request size."
  [conn input-files request-size &{:keys [check] :or [check #(count (get % :data ""))]}]
  (let [m (load-lookup-sets input-files)
        sets (create-mget-bench-input m request-size)
        start (current-time)
        result (doall (map
                        (fn [tuple]
                          (let [bucket (first tuple)
                                maps (rest tuple)
                                start (current-time)]
                            (check (jdbc-util/jdbc-mget conn bucket maps))
                            (elapsed-time start)))
                        sets))
        total (count result)
        mean (util/mean result total)
        stop (current-time)]
    {:count total
     :mean mean
     :variance (double (util/variance result mean total))
     :95th (util/percentile result 0.95)
     :elapsed (elapsed-time start stop)}))
