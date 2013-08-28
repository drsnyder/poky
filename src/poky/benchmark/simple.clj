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
  [str-ts]
  (tc/to-timestamp (tf/parse pg-default-date str-ts)))

(defn load-lookup-set
  [m file]
  (let [lines (util/read-file file)
        csv (map #(clojure.string/split % #",") lines)]
    (reduce (fn [init row]
              (update-in init
                         [(first row)]
                         conj {:key (second row) :modified_at (csv-time->timestamp (last row)) }))
            m csv)))

(defn load-lookup-sets
  [files]
  (reduce load-lookup-set {} files))

(defn create-mget-bench-input
  [m request-size]
  (let [by-bucket (map (fn [bucket]
                         ; split the k,v pairs up into the request size blocks
                         (let [blocks (partition request-size (get m bucket)) ]
                           ; add the bucket to the front of the block to identify it
                           (map #(conj % bucket) blocks)))
                       (keys m))]
    (shuffle (apply concat by-bucket))))


(comment
  (bench/bench
    (connection (system/store S))
    ["tmp/mothering.csv" "tmp/basenotes.csv" "tmp/avsforum.csv" "tmp/headfi.csv"]
    30))

(defn bench-jdbc-mget
  "Benchmark jdbc-mget."
  [conn input-files request-size &{:keys [check] :or [check #(count (get % :data ""))]}]
  (let [m (load-lookup-sets input-files)
        sets (create-mget-bench-input m request-size)
        start (. java.lang.System  (clojure.core/nanoTime))
        result (dorun (map check
                           (flatten
                             (map #(jdbc-util/jdbc-mget
                                     conn
                                     (first %)
                                     (rest %))
                                  sets))))]
    {:result result
     :elapsed (/
               (- (. java.lang.System  (clojure.core/nanoTime))
                  start)
               1000000.0)}))

