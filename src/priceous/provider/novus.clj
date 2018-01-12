(ns priceous.provider.novus
  (:require [priceous.provider.zakaz :as zakaz]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-categories [provider]
  (->> [["18+" "https://novus.zakaz.ua/ru/eighteen-plus/?&page=%s"]]
       (mapv (fn [[name url]] {:name name :template url}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
           :category :no-category
           :page-limit     Integer/MAX_VALUE
           :done           false
           :current-val    1
           :init-val       1
           :advance-fn     inc
           }

   :configuration {
                   :do-not-use-number-for-first-page true
                   :template-variable "\\?&page=%s"

                   :categories-fn      get-categories
                   :threads            1
                   :strategy           :light
                   :node->document     zakaz/node->document
                   :node-selector      zakaz/node-selector
                   :last-page-selector zakaz/last-page-selector
                   }


   })
