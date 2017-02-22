(ns priceous.provider.goodwine
  (:require [net.cgrand.enlive-html :as html]
            [taoensso.timbre :as log]
            [priceous.flow :as flow]
            [priceous.utils :as u]
            [priceous.provider :as p]
            [priceous.selector-utils :as su]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-categories [provider]
  [{:name "Виски" :template "http://goodwine.com.ua/viski.html?dir=asc&p=%s"}
   {:name "Вино" :template "http://goodwine.com.ua/vino.html?dir=asc&p=%s"}
   {:name "Игристое Вино" :template "http://goodwine.com.ua/igristye.html?dir=asc&p=%s"}
   {:name "Пиво" :template "http://goodwine.com.ua/pivo.html?dir=asc&p=%s"}
   {:name "Cидр" :template "http://goodwine.com.ua/sidry.html?dir=asc&p=%s"}
   {:name "Другие Крепкие" :template "http://goodwine.com.ua/drugie-krepkie.html?dir=asc&p=%s"}])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- node->document
  "Transform enlive node to provider specific document using node context.
  For heavy strategy, in context available whole :page, partial :node
  and :link by which whole page was retrieved.

  If scrapper finds a broken page, it returns whole doc as a nil."
  [provider {page :page node :node link :link :as nodemap}]
  (su/with-selectors provider nodemap
    ;; process only if page is available and product is valid
    (when (and page (q? [:.mainInfoProd]))
      (let [spec (->> (q* [:.innerDiv])
                      (map (fn [n]
                             (->> [(html/select n [:.innerTitleProd])
                                   (html/at n [:.innerTitleProd] nil)]
                                  (mapv first)
                                  (mapv html/text)
                                  (mapv u/cleanup))))
                      (into {}))]
        (-> {}
            (assoc :provider (p/pname provider))
            (assoc :name (text+ [:.titleProd :> [:* (html/but [:.articleProd])]]))
            (assoc :link link)
            (assoc :image (-> (q+ [:.imageDetailProd [:img :#mag-thumb]])
                              (get-in [:attrs :src])))
            (assoc :country (spec "география"))
            (assoc :wine_sugar (some-> (spec "Сахар, г/л") (u/smart-parse-double)))
            (assoc :wine_grape (some->> (spec "Сортовой состав")
                                        (#(clojure.string/split % #";"))
                                        (map u/cleanup)
                                        (remove empty?)
                                        (clojure.string/join ", ")))
            (assoc :vintage (spec "Винтаж"))
            (assoc :producer (spec "Производитель"))
            (assoc :type (-> (str (p/category-name provider) " " (spec "Тип"))
                             (u/cleanup)))
            (assoc :alcohol (-> (spec "Крепость, %") (u/smart-parse-double)))
            (assoc :description (spec "цвет, вкус, аромат"))
            (assoc :timestamp (u/now))
            (assoc :product-code (->> (list (get-in provider [:info :name])
                                            (-> (q+ [:.titleProd :.articleProd :span])
                                                (html/text)))
                                      (clojure.string/join "_")))
            (assoc :available (empty? (q? [:.notAvailableBlock])))
            (assoc :item_new  (boolean (some-> (q? [:.medalIcon :.stamp])
                                               (get-in [:attrs :src])
                                               (.contains "/New_"))))
            
            ;; volume and price blocks are present if product is available
            ((fn [p] (assoc p :volume
                            (if (:available p)
                              (-> (q* [:.additionallyServe :.bottle :p])
                                  (first)
                                  (html/text)
                                  (u/smart-parse-double))))))
            
            ((fn [p] (assoc p :price
                            (if (:available p)
                              (-> (q* [:.additionallyServe :.price])
                                  (first)
                                  (html/text)
                                  (u/smart-parse-double))))))

            ;; process sales
            (assoc :sale-description (some->> (su/select? node provider
                                                    [:.price.red] :context nodemap)
                                        (html/text)
                                        (#(clojure.string/split % #"\?"))
                                        (first)
                                        (u/smart-parse-double)
                                        (format "Цена при покупке любых 6+ бутылок, %.2f грн")))
            ((fn [p] (assoc p :sale (not (empty? (:sale-description p))))))
            
            )))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
           :page-template  "http://goodwine.com.ua/viski.html?p=%s"
           :category       :no-category
           :page-limit     Integer/MAX_VALUE
           :done           false
           }

   :configuration {
                   :categories-fn      get-categories
                   :threads            8
                   :strategy           :heavy
                   :node->document     node->document
                   :node-selector      [:.catalogListBlock :> :ul :> [:li (html/but [:.hide])]]
                   :link-selector      [:.textBlock [:a :.title]]
                   :link-selector-type :full-href
                   :last-page-selector [:.paginator [:a (html/attr-has :href)]]
                   }
   })
