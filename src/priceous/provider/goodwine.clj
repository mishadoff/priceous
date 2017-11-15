(ns priceous.provider.goodwine
  (:require [net.cgrand.enlive-html :as html]
            [taoensso.timbre :as log]
            [priceous.utils :as u]
            [priceous.provider :as p]
            [priceous.selector-utils :as su]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-categories [provider]
  (->>
    [["Виски" "http://goodwine.com.ua/viski.html"]
     ["Вино" "http://goodwine.com.ua/vino.html"]
     ["Игристое Вино" "http://goodwine.com.ua/igristye.html"]
     ["Пиво" "http://goodwine.com.ua/pivo.html"]
     ["Cидр" "http://goodwine.com.ua/sidry.html"]
     ["Ром" "http://goodwine.com.ua/drugie-krepkie/rom/vse-romy.html"]
     ["Коньяк" "http://goodwine.com.ua/drugie-krepkie/prochie-krepkie/cognac.html"]
     ["Арманьяк" "http://goodwine.com.ua/drugie-krepkie/prochie-krepkie/armagnac.html"]
     ["Бренди" "http://goodwine.com.ua/drugie-krepkie/prochie-krepkie/brandy.html"]
     ["Ликер" "http://goodwine.com.ua/drugie-krepkie/prochie-krepkie/liquer.html"]
     ["Биттер" "http://goodwine.com.ua/drugie-krepkie/prochie-krepkie/bitter.html"]
     ["Кальвадос" "http://goodwine.com.ua/drugie-krepkie/prochie-krepkie/calvados.html"]
     ["Джин" "http://goodwine.com.ua/drugie-krepkie/prochie-krepkie/gin.html"]
     ["Водка" "http://goodwine.com.ua/drugie-krepkie/prochie-krepkie/vodka.html"]
     ["Граппа" "http://goodwine.com.ua/drugie-krepkie/prochie-krepkie/grappa.html"]
     ["Текила" "http://goodwine.com.ua/drugie-krepkie/prochie-krepkie/tequilla.html"]
     ["Писко" "http://goodwine.com.ua/drugie-krepkie/prochie-krepkie/pisko.html"]
     ["Мескаль" "http://goodwine.com.ua/drugie-krepkie/prochie-krepkie/meskal.html"]]
    (mapv (fn [[name url]] {:name name :template (str url "?dir=asc&p=%s")}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn node->document
  "Transform enlive node to provider specific document using node context.
  For heavy strategy, in context available:
       :page - enlive node, whole page
       :node - enlive node, partial from the search
       :link - link by which whole page was retrieved.
  If scrapper finds a broken page, it returns whole doc as a nil."
  [provider {page :page node :node link :link :as nodemap}]
  (su/with-selectors
    provider
    nodemap
    (when (and page (q? [:.mainInfoProd]))
      (let [spec (su/spec-with-only-key (q* [:.innerDiv]) [:.innerTitleProd])]
        (-> {}
            (assoc :provider (p/pname provider))
            (assoc :excise true)
            (assoc :trusted true)
            (assoc :name (text+ [:.titleProd :> [:* (html/but [:.articleProd])]]))
            (assoc :link link)
            (assoc :image (img [:.imageDetailProd [:img :#mag-thumb]]))
            (assoc :country (spec "география"))
            (assoc :wine_sugar (some-> (spec "Сахар, г/л") (u/smart-parse-double)))
            (assoc :wine_grape (some->> (spec "Сортовой состав")
                                        (#(clojure.string/split % #";"))
                                        (map u/cleanup)
                                        (remove empty?)
                                        (clojure.string/join ", ")))
            (assoc :vintage (spec "Винтаж"))
            (assoc :producer (spec "Производитель"))
            (assoc :type (u/cat-items (p/category-name provider) (spec "Тип")))
            (assoc :alcohol (u/smart-parse-double (spec "Крепость, %")))
            (assoc :description (spec "цвет, вкус, аромат"))
            (assoc :timestamp (u/now))
            (assoc :product-code (str (p/pname provider) "_" (text+ [:.titleProd :.articleProd :span])))
            (assoc :available (empty? (q? [:.notAvailableBlock])))

            (assoc :item_new (->> (q*? [:.medalIcon :.stamp]) ;; TODO more medals
                                  (map #(.contains (get-in % [:attrs :src]) "/New_"))
                                  (some true?)))

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
            (assoc :sale-description (some->> (q*? [:.bottle.pull-left :span])
                                              (map html/text)
                                              (filter #(.contains % "За 1 бут."))
                                              (first)
                                              ((fn [txt] (.split txt "—")))
                                              (seq)
                                              (second)
                                              (u/smart-parse-double)
                                              (format "Цена при покупке любых 6+ бутылок, %.2f грн")))

            ((fn [p] (assoc p :sale (not (empty? (:sale-description p)))))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def provider
  {
   :info {
          :name          "Goodwine"
          :base-url      "http://goodwine.com.ua/"
          :icon          "http://goodwine.com.ua/skin/frontend/default/goodwine/assets/img/new_logo.svg"
          :icon-background "black"
          :icon-width    "90"
          :icon-height   "50"}


   ;; provider state, will be changed by flow processor
   :state {
           :page-current   1
           :page-processed 0
           :page-template  "http://goodwine.com.ua/viski.html?p=%s"
           :category       :no-category ;; we change categories in runtime
           :page-limit     Integer/MAX_VALUE
           :done           false
           :current-val    1
           :init-val       1
           :advance-fn     inc}

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