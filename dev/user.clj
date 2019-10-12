(ns user
  (:require [priceous.system.lifecycle :as system]))

(def start system/start)
(def stop system/stop)

(defn init []
  (start)
  :ready)

(defn reset []
  (stop)
  (init))