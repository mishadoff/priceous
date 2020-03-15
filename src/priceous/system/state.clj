(ns priceous.system.state)

(defonce system nil)

(defn db []
  (-> system :db/postgres))
