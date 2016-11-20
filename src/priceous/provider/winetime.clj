(ns priceous.provider.winetime
  (:require [net.cgrand.enlive-html :as html]
            [taoensso.timbre :as log]
            [priceous.flow :as flow]
            [priceous.utils :as u]
            [priceous.selector-utils :as su]))

(defn- next-page? [provider page]
  (let [last-page 
        (some->> (su/select-mul-opt page provider
                                    [:.pagination :.pag])
                 (map html/text)
                 (remove #{"»" "..."})
                 (map u/smart-parse-double)
                 (sort)
                 (last)
                 (int))]
    ;; default page is 1
    (< (get-in provider [:state :page-current])
       (or last-page 1))))


(defn- page->urls [provider page]
  (->> ((su/generic-page-urls [:.item-block-head_main :a])
        provider page)
       (map #(u/full-href provider %))))

(def cnt (atom 0))
;;; 10 threads for winetime...
;;; Some problems with spec maps
(defn- url->document
  "Read html resource from URL and transforms it to the document"  
  [provider url]
  (if ((get-in provider [:skip :url] #{}) url)
    (do (log/warn "Skipping URL: " url) nil)
    (let [page (u/fetch url) ;; retrieve the page
          prop (su/property-fn provider page)
          text (su/text-fn prop)
          spec (su/build-spec-map provider page
                                  [:table.details_about [:tr html/first-child] :td :p]
                                  [:table.details_about [:tr html/first-child] :td :p :a]
                                  :keys-post-fn (fn [node]
                                                 (let [txt (first (:content node))]
                                                   (assoc-in node [:content] (list txt)))))
          spec2 (su/build-spec-map provider page
                                  [:.harakter_tovar :> :p :> :strong]
                                  [:.harakter_tovar :> :p]
                                  :vals-post-fn (fn [node]
                                                 (let [rst (rest (:content node))]
                                                   (assoc-in node [:content] rst))))
          price (some-> (text [:.show_all_sum])
                        (u/smart-parse-double)
                        (/ 100.0))
          old-price (some-> (su/select-mul-opt page provider [:.buying_block_compare :span])
                            (first)
                            (html/text)
                            (u/smart-parse-double))]
      {
       ;; provider specific options
       :provider                (get-in provider [:info :name])
       :base-url                (get-in provider [:info :base-url])
       :icon-url                (get-in provider [:info :icon])
       :icon-url-width          (get-in provider [:info :icon-width])
       :icon-url-height         (get-in provider [:info :icon-height])
     
       ;; document
       :name                    (text [:.product-details-wraper :h1])
       :link                    url
       :image                   (-> (prop [:.foto_main :a :img])
                                    (get-in [:attrs :src])
                                    (#(u/full-href provider %)))
       :country                 (str (spec "Страна:") " " (spec "Регион:"))
       :type                    (str (spec "Тип:") " " (spec "Классификация:"))
       :producer                (spec "Производитель:")
       :timestamp               (u/now)
       :volume                  (-> (text [:.product-details_info-block :.size])
                                    (u/smart-parse-double))
       :alcohol                 (-> (spec2 "Алкоголь") (u/smart-parse-double))
       :description             (spec2 "Дегустации")

       :available               (-> (prop [:.buying_block_do])
                                    (boolean))

       :price                   price
       :old-price               old-price
       :sale                    (boolean old-price)
       :sale-description        (if old-price (format "старая цена %.2f" old-price))

     })))


(def provider
  {
   ;; provider specific information
   :info {
          :name "Winetime"
          :base-url "http://winetime.com.ua"
          :icon "http://winetime.com.ua/shared/site/images/logo_03.jpg"
          :icon-width "119"
          :icon-height "34"
          }
   
   ;; provider state, will be changed by flow processor
   :state {
           :page-current   1
           :page-processed 0
           ;; TODO implement paging for winetime using increment step
           :page-template "http://winetime.com.ua/alcohol/?size=10000"
           :page-limit     1
           :done           false
           }

   ;; fetch strategy defines how we will fetch results
   :fetch-strategy :heavy
   :category false
   
   :functions {
               :url->document url->document
               :page->urls    page->urls
               :last-page?    next-page?
               }
   
   })
