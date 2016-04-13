(ns priceous.fozzy
  (:require [priceous.zakaz :as zakaz]))

;; Preconditions set-driver
(defn whiskey-prices []
  (let [base "https://fozzy.zakaz.ua/ru/whiskey"]
    (println "Processing Fozzy...")
    (zakaz/process base)))
