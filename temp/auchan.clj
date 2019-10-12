(ns priceous.provider.auchan
  (:require [priceous.provider.zakaz-react :as zr]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def provider
  {
   :info {
          :name "Auchan"
          :base-url "https://auchan.zakaz.ua"
          :icon "https://auchan.ua/skin/frontend/auchan/default/images/media/logo.png"
          :icon-width "171"
          :icon-height "54"
          }

   :custom {
            :store_num "48246401"
            }
   
   :state {:page-current   1
           :page-processed 0
           :page-template  "https://auchan.zakaz.ua/api/query.json"
           :page-limit     Integer/MAX_VALUE
           :done           false
           :current-val    1
           :init-val       1
           :advance-fn     inc
           }
   
   :configuration {
                   :threads   1
                   :strategy  :api
                   :api-fn    zr/query
                   }
   })
