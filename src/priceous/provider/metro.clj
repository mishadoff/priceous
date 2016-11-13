(ns priceous.provider.metro
  (:require [priceous.provider.zakaz :as zakaz]))

(def last-page zakaz/last-page)
(def page->nodes zakaz/page->nodes)
(def node->document zakaz/node->document)

;;;;;;; Provider description

(def provider
  {:info {
          :name "Metro"
          :base-url "https://metro.zakaz.ua"
          :icon "/images/metro.png"
          :icon-width "96"
          :icon-height "27"
          }

   :state {:page-current   1
           :page-processed 0
           :page-template "https://metro.zakaz.ua/ru/whiskey/?&page=%s"
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
