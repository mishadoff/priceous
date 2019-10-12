(ns priceous.provider.metro
  (:require [priceous.provider.zakaz-react :as zr]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def provider
  {:info {
          :name "Metro"
          :base-url "https://metro.zakaz.ua/"
          :icon "/images/metro.png"
          :icon-width "96"
          :icon-height "27"
          }

   :custom {
            :store_num "48215611"
            }
   
   :state {:page-current   1
           :page-processed 0
           :page-template  "https://metro.zakaz.ua/ru/whiskey/?&page=%s"
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
