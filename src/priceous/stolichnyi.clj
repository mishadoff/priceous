(ns priceous.stolichnyi
  (:require [priceous.zakaz :as zakaz]))

(defn process []
  (zakaz/process
   {:provider "Stolichnyi"
    :base "https://stolichnyi.zakaz.ua/ru/whiskey"}))
