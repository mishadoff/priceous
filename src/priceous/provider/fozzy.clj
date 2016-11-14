(ns priceous.provider.fozzy
  (:require [priceous.provider.zakaz :as zakaz]))

(def last-page zakaz/last-page)
(def page->nodes zakaz/page->nodes)
(def node->document zakaz/node->document)

;;;;;;; Provider description

(def provider
  {:info {
          :name "Fozzy"
          :base-url "https://fozzy.zakaz.ua"
          :icon "http://www.fozzy.ua/include/img/fozzy_logo.png"
          :icon-width "70"
          :icon-height "34"
          }

   :state {:page-current   1
           :page-processed 0
           :page-template "https://fozzy.zakaz.ua/ru/hard-drinks/?&page=%s"
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
