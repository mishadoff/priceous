(ns priceous.scrap
  (:require [net.cgrand.enlive-html :as html]))

(def ^:dynamic *base-url* "https://metro.zakaz.ua/ru/whiskey/")

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))

(defn get-attr [node attr]
  (-> node (get :attrs) (get attr)))
  
(defn test-gw []
  (-> "http://goodwine.com.ua/whisky/c4502/page=3/"
      (fetch-url)
      
      
      ;; 
      ))
