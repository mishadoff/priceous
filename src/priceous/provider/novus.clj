(ns priceous.provider.novus
  (:require [priceous.provider.zakaz :as zakaz]))

(def last-page zakaz/last-page)
(def page->nodes zakaz/page->nodes)
(def node->document zakaz/node->document)

;;;;;;; Provider description

(def provider
  {:info {
          :name "Novus"
          :base-url "https://novus.zakaz.ua"
          :icon "http://novus.com.ua/sites/all/themes/novus/images/logo.png"
          :icon-width "112"
          :icon-height "26"
          }

   :state {:page-current   1
           :page-processed 0
           :page-template "https://novus.zakaz.ua/ru/whiskey/?&page=%s"
           :page-limit     Integer/MAX_VALUE
           :done           false
           }

   :fetch-strategy :light

   :functions {
               :node->document node->document
               :page->nodes page->nodes
               :last-page last-page
               }
   
   })
