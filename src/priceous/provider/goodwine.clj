(ns priceous.provider.goodwine
  (:require [net.cgrand.enlive-html :as html]
            [taoensso.timbre :as log]
            [priceous.flow :as flow]
            [priceous.utils :as u]
            [priceous.selector-utils :as su]
            ))

(defn- next-page? [provider page]
  (let [last-page 
        (some->> (su/select-mul-req page provider
                                    [:.paginator [:a (html/attr-has :href)]])
                 (map html/text)
                 (remove #{"»" "..."})
                 (map u/smart-parse-double)
                 (sort)
                 (last)
                 (int))]
    ;; default page is 1
    (< (get-in provider [:state :page-current])
       (or last-page 1))))
  
(def ^{:private true} page->urls
  (su/generic-page-urls [:.catalogListBlock [:a :.title]]))

(defn- url->document
  "Read html resource from URL and transforms it to the document"  
  [provider url]
  #_(log/debug "Document from URL: " url)
  (if ((get-in provider [:skip :url] #{}) url)
    (do (log/warn "Skipping URL: " url) nil)
    (let [page (u/fetch url) ;; retrieve the page
          q+ (fn [selector]
               (su/select-one-req page provider selector))
          q* (fn [selector]
               (su/select-mul-req page provider selector))
          remove-h2-node
          (fn [node]
            ;; remove h2.innerTitleProd from content
            (assoc node :content (remove #(and map?
                                               (= (get % :tag) :h2)
                                               (= (get-in % [:attrs :class] "innerTitleProd")))
                                         (:content node))))
          ;; some handy local aliases
          prop (su/property-fn provider page)
          text (su/text-fn prop)
          spec-left (su/build-spec-map
                     provider page
                     [:.i-colLeft :.innerDiv [:h2 :.innerTitleProd]]
                     [:.i-colLeft :.innerDiv]
                     :vals-post-fn remove-h2-node)
          spec-right (su/build-spec-map
                      provider page
                      [:.i-colRight :.innerDiv [:h2 :.innerTitleProd]]
                      [:.i-colRight :.innerDiv]
                      :vals-post-fn remove-h2-node)
          spec (merge spec-left spec-right)
          available (empty? (su/select-one-opt page provider [:.notAvailableBlock]))
          ]
    {
     ;; provider specific options
     :provider                (get-in provider [:info :name])
     :base-url                (get-in provider [:info :base-url])
     :icon-url                (get-in provider [:info :icon])
     :icon-url-width          (get-in provider [:info :icon-width])
     :icon-url-height         (get-in provider [:info :icon-height])
     
     ;; document
     :name                    (text [:.titleProd])
     :link                    url
     :image                   (-> (q+ [:.imageDetailProd [:img :#mag-thumb]])
                                  (get-in [:attrs :src]))
     :country                 (spec "география")
     :producer                (spec "Производитель")
     :type                    (spec "Тип")
     :alcohol                 (-> (spec "Крепость, %") (u/smart-parse-double))
     :description             (spec "цвет, вкус, аромат")
     :timestamp               (u/now)
     :product-code            (-> (q+ [:.titleProd :.articleProd :span]) (html/text))
     :available               available
     :volume                  (if available
                                (-> (q* [:.additionallyServe :.bottle :p])
                                    (first)
                                    (html/text)
                                    (u/smart-parse-double)))
     :price                   (if available
                                (-> (q* [:.additionallyServe :.price])
                                    (first)
                                    (html/text)
                                    (u/smart-parse-double)))
     :sale-description        nil
     :sale                    false

     })))


(defn get-categories [provider]
  [["Виски" "http://goodwine.com.ua/viski.html?dir=asc&p=%s"]
   ;; Not enabled yet
   ["Другие Крепкие" "http://goodwine.com.ua/drugie-krepkie.html?dir=asc&p=%s"]])

;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;  PROVIDER  ;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;


(def provider
  {
   ;; provider specific information
   :info {
          :name          "Goodwine"
          :base-url      "http://goodwine.com.ua/"
          :icon          "http://i.goodwine.com.ua/design/goodwine-logo.png"
          :icon-width    "70"
          :icon-height   "34"
          }
   
   ;; provider state, will be changed by flow processor
   :state {
           :page-current   1
           :page-processed 0
           :page-template "http://goodwine.com.ua/viski.html?p=%s"
           :page-limit     Integer/MAX_VALUE
           :done           false
           }

   ;; fetch strategy defines how we will fetch results
   :fetch-strategy :heavy
   :category true
   
   :functions {
               :url->document url->document
               :page->urls    page->urls
               :last-page?    next-page?

               :categories    get-categories ;; return name [tempalte]
               }

   ;;:skip {:url #{"http://goodwine.com.ua/armagnac/p53825/" "http://goodwine.com.ua/sambuca/p45499/"}}
   
   })
