(ns poky.kv.jdbc.util
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(def ^:private default-pool-size 3)

(defn create-db-spec
  ([dsn driver]
   (let [uri (java.net.URI. dsn)
         host (.getHost uri)
         scheme (.getScheme uri)
         [user pass] (clojure.string/split (.getUserInfo uri) #":")
         port (.getPort uri)
         port (if (= port -1) 5432 port)
         path (.substring (.getPath uri) 1)]
     {:classname driver
      :subprotocol scheme
      :subname (str "//" host ":" port "/" path)
      :user user
      :password pass}))
  ([dsn]
   (create-db-spec dsn "org.postgresql.Driver")))

(defn pool
  [spec &{:keys [min-pool-size] :or {min-pool-size default-pool-size}}]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname spec)) 
               (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
               (.setUser (:user spec))
               (.setPassword (:password spec))
               (.setMinPoolSize min-pool-size)
               (.setMaxIdleTimeExcessConnections (* 30 60))
               (.setMaxIdleTime (* 3 60 60)))] 
      {:datasource cpds}))
