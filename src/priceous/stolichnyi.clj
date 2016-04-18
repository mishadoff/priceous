(ns priceous.stolichnyi
  (:require [priceous.zakaz :as zakaz]))

(def flow
  (zakaz/->ZakazFlow
   {:provider "Stolichnyi"
    :base "https://stolichnyi.zakaz.ua/ru/whiskey"}))
