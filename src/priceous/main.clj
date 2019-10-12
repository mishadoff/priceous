(ns priceous.main
  (:require [priceous.system.lifecycle :as system])
  (:gen-class))

(defn -main [& args]
  (system/start))
