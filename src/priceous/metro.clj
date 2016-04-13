(ns priceous.metro
  (:require [priceous.zakaz :as zakaz]))

;; Preconditions set-driver
(defn whiskey-prices []
  (let [base "https://metro.zakaz.ua/ru/whiskey"]
    (println "Processing Metro...")
    (zakaz/process base)))
