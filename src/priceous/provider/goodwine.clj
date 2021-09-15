(ns priceous.provider.goodwine
  (:require [net.cgrand.enlive-html :as html]
            [priceous.spider.provider :as p]
            [priceous.spider.selector-utils :as su]
            [priceous.utils.time :as time]
            [priceous.utils.numbers :as numbers]
            [priceous.utils.collections :as collections]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-categories [provider]
  (->>
    [["Виски" "https://goodwine.com.ua/viski.html"]
     ["Вино" "https://goodwine.com.ua/vino.html"]
     ["Игристое Вино" "https://goodwine.com.ua/igristye.html"]
     ["Пиво" "https://goodwine.com.ua/pivo.html"]
     ["Cидр" "https://goodwine.com.ua/sidry.html"]
     ["Ром" "https://goodwine.com.ua/drugie-krepkie/rom/vse-romy.html"]
     ["Коньяк" "https://goodwine.com.ua/drugie-krepkie/prochie-krepkie/cognac.html"]
     ["Арманьяк" "https://goodwine.com.ua/drugie-krepkie/prochie-krepkie/armagnac.html"]
     ["Бренди" "https://goodwine.com.ua/drugie-krepkie/prochie-krepkie/brandy.html"]
     ["Ликер" "https://goodwine.com.ua/drugie-krepkie/prochie-krepkie/liquer.html"]
     ["Биттер" "https://goodwine.com.ua/drugie-krepkie/prochie-krepkie/bitter.html"]
     ["Кальвадос" "https://goodwine.com.ua/drugie-krepkie/prochie-krepkie/calvados.html"]
     ["Джин" "https://goodwine.com.ua/drugie-krepkie/prochie-krepkie/gin.html"]
     ["Водка" "https://goodwine.com.ua/drugie-krepkie/prochie-krepkie/vodka.html"]
     ["Граппа" "https://goodwine.com.ua/drugie-krepkie/prochie-krepkie/grappa.html"]
     ["Текила" "https://goodwine.com.ua/drugie-krepkie/prochie-krepkie/tequilla.html"]
     ["Писко" "https://goodwine.com.ua/drugie-krepkie/prochie-krepkie/pisko.html"]
     ["Мескаль" "https://goodwine.com.ua/drugie-krepkie/prochie-krepkie/meskal.html"]]
    ;; FIXME: temp process only whisky category
    #_[["Виски" "https://goodwine.com.ua/viski.html"]]

    (mapv (fn [[name url]] {:name name :template (str url "?dir=asc&p=%s&order=producer")}))))

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
            (assoc :product-code (str (p/pname provider)
                                      "_"
                                      (text+ [:.articleProd [:span (html/attr= :itemprop "sku")]])))
            (assoc :link link)
            (assoc :image (img [:.imageDetailProd [:img :#mag-thumb]]))
            (assoc :country (spec "география"))
            (assoc :producer (spec "Производитель"))
            (assoc :description (spec "цвет, вкус, аромат"))
            (assoc :alcohol (numbers/smart-parse-double (spec "Крепость, %")))
            (assoc :type (collections/cat-items (p/category-name provider) (spec "Тип")))
            (assoc :timestamp (time/now))

            ;; FIXME: test if works
            (assoc :wine_sugar (some-> (spec "Сахар, г/л") (numbers/smart-parse-double)))

            ;; FIXME: test if works
            (assoc :wine_grape (some->> (spec "Сортовой состав")
                                        (#(clojure.string/split % #";"))
                                        (map collections/cleanup)
                                        (remove empty?)
                                        (clojure.string/join ", ")))

            ;; FIXME: test if works
            (assoc :vintage (spec "Винтаж"))

            (assoc :product-code (str (p/pname provider) "_" (text+ [:.articleProd [:span (html/attr= :itemprop "sku")]])))
            (assoc :available (empty? (q? [:.notAvailableBlock])))
            (assoc :item_new (->> (q*? [:.medalIcon :.stamp]) ;; TODO more medals
                                  (map #(.contains (get-in % [:attrs :src]) "/New_"))
                                  (some true?)))

            ;; volume and price blocks are present if product is available
            ((fn [p] (assoc p :volume
                              (-> (q*? [:.bottle :p])
                                  (first)
                                  (html/text)
                                  (numbers/smart-parse-double)))))

            ((fn [p] (assoc p :price
                              (-> (q* [[:meta (html/attr= :itemprop "price")]])
                                  (first)
                                  (get-in [:attrs :content])
                                  (numbers/smart-parse-double)))))

            ;; process sales
            (assoc :sale-description (some->> (q*? [:.bottle.pull-left :span])
                                              (map html/text)
                                              (filter #(.contains % "За 1 бут."))
                                              (first)
                                              ((fn [txt] (.split txt "—")))
                                              (seq)
                                              (second)
                                              (numbers/smart-parse-double)
                                              (format "Цена при покупке любых 6+ бутылок, %.2f грн")))

            ((fn [p] (assoc p :sale (not (empty? (:sale-description p)))))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def provider
  {
   :info {
          :name          "Goodwine"
          :base-url      "https://goodwine.com.ua/"
          :icon          "https://goodwine.ua/sites/all/themes/gw/img/logo_black.png"
          ;;:icon-background "black"
          :icon-width    "90"
          :icon-height   "50"}


   ;; provider state, will be changed by flow processor
   ;; todo: get rid of this from provider configuration and move to flow dynamic init
   ;; todo: split paging configuration and state
   :state {
           :page-current   1
           :page-processed 0
           :page-template  "https://goodwine.com.ua/viski.html?p=%s"
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
                   :node-selector      [:.textBlock]
                   :link-selector      [:.textBlock [:a :.title]]
                   :link-selector-type :full-href
                   :paging             :increment-until-no-changes
                   :last-page-selector [:.paginator [:a (html/attr-has :href)]]}})

