(ns priceous.novus
  (:require [priceous.zakaz :as zakaz]))

(def flow
  (zakaz/->ZakazFlow
   {:provider "Novus"
    :base "https://novus.zakaz.ua/ru/whiskey"}))
