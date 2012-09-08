(ns poky.vars)


(def ^:dynamic *connection* nil)

(def ^:dynamic *table* (get (System/getenv) "POKY_TABLE" "poky"))
(def ^:dynamic *user* (get (System/getenv) "POKY_USER" "postgres"))
(def ^:dynamic *password* (get (System/getenv) "POKY_PASSWORD" ""))

(def ^:dynamic *subname* 
  (get (System/getenv) "POKY_SUBNAME" (format "127.0.0.1:5432/%s" *table*)))
