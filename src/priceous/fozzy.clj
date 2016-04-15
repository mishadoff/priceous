(ns priceous.fozzy
  (:require [priceous.zakaz :as zakaz]))

(defn process []
  (zakaz/process
   {:provider "Fozzy"
    :base "https://fozzy.zakaz.ua/ru/whiskey"}))
