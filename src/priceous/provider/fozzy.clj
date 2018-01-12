(ns priceous.provider.fozzy
  (:require [priceous.provider.zakaz :as zakaz]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-categories [provider]
  (->> [["Алкоголь" "https://fozzy.zakaz.ua/ru/?q=алкоголь&page=%s"]]
       (mapv (fn [[name url]] {:name name :template url}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
           :category :no-category
           :page-template "https://fozzy.zakaz.ua/ru/hard-drinks/?&page=%s"
           :page-limit     Integer/MAX_VALUE
           :done           false
           :current-val    1
           :init-val       1
           :advance-fn     inc
           }

   :configuration {
                   :do-not-use-number-for-first-page true
                   :template-variable "&page=%s"
                   
                   :categories-fn      get-categories
                   :threads            1
                   :strategy           :light
                   :node->document     zakaz/node->document
                   :node-selector      zakaz/node-selector
                   :last-page-selector zakaz/last-page-selector
                   }
   })
