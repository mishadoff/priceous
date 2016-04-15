(ns priceous.novus
  (:require [priceous.zakaz :as zakaz]))

(defn process []
  (zakaz/process
   {:provider "Novus"
    :base "https://novus.zakaz.ua/ru/whiskey"}))
