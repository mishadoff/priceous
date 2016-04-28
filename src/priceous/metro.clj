(ns priceous.metro
  (:require [net.cgrand.enlive-html :as html]
            [taoensso.timbre :as log]
            [priceous.flow :as flow]
            [priceous.zakaz :as zakaz]
            [priceous.utils :as u]))

(def last-page zakaz/last-page)
(def page->nodes zakaz/page->nodes)
(def node->document zakaz/node->document)

;;;;;;; Provider description

(def provider
  {:provider-name "Metro"
   :provider-base-url "https://metro.zakaz.ua"
   :provider-icon "http//i.zakaz.ua/Metro/metro_logo.png"
   :provider-icon-width 88
   :provider-icon-height 24
   

   :state {:page-current   1
           :page-processed 0
           :page-template "https://metro.zakaz.ua/ru/whiskey/?&page=%s"
           :page-limit     Integer/MAX_VALUE
           :done           false
           }

   :strategy :light

   :page->nodes page->nodes
   :node->document node->document
   :last-page  last-page
   
   })
