(ns poky.vars)


(def connection (atom nil))

(def config (atom {}))

(def ^:dynamic *jdbc-url* (get (System/getenv) "POKY_JDBC_URL" 
                               "jdbc:postgresql://localhost:5432/poky"))

(def ^:dynamic *jdbc-driver* (get (System/getenv) "POKY_JDBC_DRIVER" 
                                  "org.postgresql.Driver"))

(def ^:dynamic *user* (get (System/getenv) "POKY_USER" "postgres"))
(def ^:dynamic *password* (get (System/getenv) "POKY_PASSWORD" ""))

(def ^:dynamic *table* (get (System/getenv) "POKY_TABLE" "poky"))

(defn set-config!
  ([]
   (set-config! *jdbc-url* *user* *password* *table* *jdbc-driver*))
  ([url user password table] 
   (set-config! url user password table *jdbc-driver*))
  ([url user password table driver] 
   (reset! config
           {:jdbc-url url
            :user user
            :password password
            :table table
            :driver driver})))

(defn set-connection! 
  [con]
  (reset! connection con))

(defn get-config [] @config)
(defn get-connection [] @connection)
(defn get-table [] (:table @config))
