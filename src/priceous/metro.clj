(ns priceous.metro
  (:require [priceous.zakaz :as zakaz]))

(defn process []
  (zakaz/process
   {:provider "Metro"
    :base "https://metro.zakaz.ua/ru/whiskey"}))
