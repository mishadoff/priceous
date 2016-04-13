(ns priceous.stolichnyi
  (:require [priceous.zakaz :as zakaz]))

;; Preconditions set-driver
(defn whiskey-prices []
  (let [base "https://stolichnyi.zakaz.ua/ru/whiskey"]
    (println "Processing Stolychnyi Rynok...")
    (zakaz/process base)))
