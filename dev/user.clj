(ns user
  (:require [priceous.system :as system]))

(def start system/start)
(def stop system/stop)

(defn go []
  (start)
  :ready)

(defn reset []
  (stop)
  (go))