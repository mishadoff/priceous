(ns priceous.metro
  (:require [priceous.zakaz :as zakaz]))

(def flow
  (zakaz/->ZakazFlow
   {:provider "Metro"
    :base "https://metro.zakaz.ua/ru/whiskey"}))
