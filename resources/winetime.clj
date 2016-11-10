(ns priceous.winetime
  (:require [net.cgrand.enlive-html :as html]
            [priceous.flow :as flow]
            [priceous.utils :as u]
            ))

(defn page->urls [provider page]
  ((u/generic-page-urls-with-prefix
    [:.item-block-head_main :a]
    (:provider-base-url provider)) provider page))

(defn url->document
  "Transform item html snippet (node) into document"  
  [{:keys [provider-name provider-base-url provider-icon
           provider-icon-w provider-icon-h] :as provider} url]
  (let [page (u/fetch url)
        ;; some handy local aliases
        prop (u/property-fn provider page)
        text (u/text-fn prop)]
    {
     ;; provider specific options
     ;; TODO: this often repeats, fix
     :provider-name           provider-name
     :provider-base-url       provider-base-url
     :provider-icon           provider-icon
     :provider-icon-w         provider-icon-w
     :provider-icon-h         provider-icon-h
     
     ;; document
     :name                    (text [:.product-details-wrapper :h1])
     :link                    url
     :image                   (-> (prop [:.product-details-wrapper :.foto_main :img])
                                  (get-in [:attrs :src])
                                  ((fn [part-href]
                                     (str (:provider-base-url provider) part-href))))

     ;; NOT DECIDED TO GATHER YET
     ;;
     ;;     :country                 
     ;; 
     ;;     :producer
     :type                    "Whisky" ;; TODO redecide
     ;; :product-code            nil
     ;; :alcohol                 need to be parsed
     ;; :description             not much description
     :timestamp               (u/now)
     :available               true ;; zakaz everything shows as available
     ;;     :volume              need to be parsed

     :sale                    nil
     
     :price                   (-> (text [:.show_all_sum])
                                  (u/smart-parse-double))

     :sale-description        nil
     }))

(def provider
  {:provider-name "Winetime"
   :provider-base-url "http://winetime.com.ua"
   :provider-icon "http://winetime.com.ua/shared/site/images/logo_03.jpg"
   :provider-icon-w "119"
   :provider-icon-h "34"

   :state {:page-current   1
           :page-processed 0
           :page-template "http://winetime.com.ua/ua/whiskey.htm?type_tovar=view_all_tovar&size=10000"
           :page-limit     1 ;Integer/MAX_VALUE
           :done           false
           }

   :strategy :heavy
   
   :page->urls page->urls
   :url->document url->document
   :last-page  (fn [_ _] true) ;; only one page processing for now, since we use hack

   })
