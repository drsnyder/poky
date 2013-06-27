(ns ring.middleware.new-relic
  (:import com.newrelic.api.agent.NewRelic))

(defn- format-tx-name
  "Returns formatted transaction name. Currently only adds leading forward
  slash if missing"
  [tx-name]
  (if (not= \/ (first tx-name))
    (str "/" tx-name)
    tx-name))

(defn wrap-transaction-name
  "Middleware for calling com.newrelic.api.agent.NewRelic/setTransactionName
  Category name is logical grouping for transaction. Allowed to be null.
  Transaction name is should be a URI path format (ie start with forward slash)
  Options:
  :category Use constant string for category
  :tx-name  Use constant string for transaction name
  :category-fn (category-fn request) is invoked to obtain category name
  :tx-name-fn  (tx-name-fn request) is invoked to obtain transaction name"
  [handler & {:keys [category category-fn tx-name tx-name-fn]}]
  (let [category-fn (or category-fn (constantly category))
        tx-name-fn (or tx-name-fn (constantly tx-name))]
    (fn [request]
      (if-let [tx-name (tx-name-fn request)]
        (NewRelic/setTransactionName (category-fn request)
                                     (format-tx-name tx-name)))
      (handler request))))

;; TODO add middleware for recordMetric
;; TODO add middleware for recordResponseTimeMetric
;; TODO add middleware for incrementCounter
