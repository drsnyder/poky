(ns poky.vars)

(def ^:dynamic *table* "poky")

(def ^:dynamic *connection* (get (System/getenv) "DATABASE_URL"))
