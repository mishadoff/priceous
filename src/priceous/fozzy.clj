(ns priceous.fozzy
  (:require [priceous.zakaz :as zakaz]))

(def last-page zakaz/last-page)
(def page->nodes zakaz/page->nodes)
(def node->document zakaz/node->document)

;;;;;;; Provider description

(def provider
  {:provider-name "Fozzy"
   :provider-base-url "https://fozzy.zakaz.ua"
   :provider-icon "" ;; TODO
   :provider-icon-width 88
   :provider-icon-height 24
   

   :state {:page-current   1
           :page-processed 0
           :page-template "https://fozzy.zakaz.ua/ru/whiskey/?&page=%s"
           :page-limit     Integer/MAX_VALUE
           :done           false
           }

   :strategy :light

   :page->nodes page->nodes
   :node->document node->document
   :last-page  last-page
   
   })
