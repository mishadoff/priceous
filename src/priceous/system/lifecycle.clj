(ns priceous.system.lifecycle
  (:require [integrant.core :as ig]
            [priceous.system.state :as system]
            [priceous.system.components :as components]))

;;;

(defn stop []
  (when system/system
    (ig/halt! system/system)))

;;;

(defn start []
  (alter-var-root #'system/system
                  (fn [prev-system]
                    (stop)
                    (ig/init components/declaration))))