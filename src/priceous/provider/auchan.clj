(ns priceous.provider.auchan
  (:require [priceous.provider.zakaz-react :as zr]))

(def provider
  {
   :info {
          :name "Auchan"
          :base-url "https://auchan.zakaz.ua/"
          :icon "https://auchan.zakaz.ua/s/auchan/img/auchan_logo.png"
          :icon-width "134"
          :icon-height "34"
          }

   :custom {
            :store_num "48246401"
            }

   :state {
           :page-current   1
           :page-processed 0
           :page-template  "https://auchan.zakaz.ua/api/query.json"
           :page-limit     Integer/MAX_VALUE
           :done           false
           }

   :fetch-strategy :api
   
   :functions { :docs zr/query }
   
   

   }





  )

