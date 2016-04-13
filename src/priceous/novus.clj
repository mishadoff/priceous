(ns priceous.novus
  (:require [priceous.zakaz :as zakaz]))

;; Preconditions set-driver
(defn whiskey-prices []
  (let [base "https://novus.zakaz.ua/ru/whiskey"]
    (println "Processing Novus...")
    (zakaz/process base)))
