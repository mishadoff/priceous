(ns priceous.system
  (:require [integrant.core :as ig]
            [priceous.components :as components]))

(defonce system nil)

;;;

(defn stop []
  (when system
    (ig/halt! system)))

;;;

(defn start []
  (alter-var-root #'system
                  (fn [prev-system]
                    (stop)
                    (ig/init components/declaration))))