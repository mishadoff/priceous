(ns priceous.novus
  (:require [priceous.zakaz :as zakaz]))

(def last-page zakaz/last-page)
(def page->nodes zakaz/page->nodes)
(def node->document zakaz/node->document)

;;;;;;; Provider description

(def provider
  {:provider-name "Novus"
   :provider-base-url "https://novus.zakaz.ua"
   :provider-icon "http://novus.com.ua/sites/all/themes/novus/images/logo.png"
   :provider-icon-w "112"
   :provider-icon-h "26"
   

   :state {:page-current   1
           :page-processed 0
           :page-template "https://novus.zakaz.ua/ru/whiskey/?&page=%s"
           :page-limit     Integer/MAX_VALUE
           :done           false
           }

   :strategy :light

   :page->nodes page->nodes
   :node->document node->document
   :last-page  last-page
   
   })
