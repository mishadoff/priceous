(ns priceous.fozzy
  (:require [priceous.zakaz :as zakaz]))

(def flow
  (zakaz/->ZakazFlow
   {:provider "Fozzy"
    :base "https://fozzy.zakaz.ua/ru/whiskey"}))
