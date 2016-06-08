(ns priceous.template
  (:require [hiccup.core :refer :all]
            [hiccup.form :refer :all]
            [priceous.utils :as u]
            [hiccup.page :as page]))

(declare
 provider-element
 sale-element
 status-bar
 price-element
 render-item)

(defn- sale-element [provider]
  (let [{:keys [sale sale_description]} provider]
    (if sale
      [:div {:class "sale-description"} "Акция"])))

(defn- status-bar [content]
  (cond
    (= :error (get-in content [:response :status]))
    [:div {:class "status-bar-error"}
     "Ошибка: Неправильный запрос"]

    ;; Query not entered, first time here
    (empty? (:query content))
    [:div {:class "status-bar-small"}
     "Введите запрос, например "
     [:a {:href "/search?query=springbank"
          :class "amber"}
      "springbank"]]

    ;; Query entered, but nothing found
    (and (not (empty? (:query content)))
         (zero? (get-in content [:response :data :response :numFound])))
    [:div {:class "status-bar-regular"}
     "Ничего не найдено."]
    
    :else
    [:div {:class "status-bar-small"}
     (format "Найдено %s товаров за %s секунд. "
             (get-in content [:response :data :response :numFound])
             (some-> (get-in content [:response :data :responseHeader :QTime])
                     (/ 1000.0)))
     (if (> (get-in content [:response :data :response :numFound] 0)
            (count (get-in content [:response :data :response :docs] [])))
       [:span {:class "status-bar-warning"}
        "(показано только первых 50 товаров)"])
     
     ]))

(defn price-element [price]
  (cond
    price
    (let [grn (bigint (Math/floor price))
          kop (->> (int (* 100 (- price grn)))
                   (format "%2d")
                   ((fn [s] (clojure.string/replace s " " "0"))))]
      [:div
       [:span {:class "grn"} grn]
       [:span {:class "kop"} kop]])

    :esle
    [:div {:class "price-na"} "-"]
    ))

(defn render-item
  "Renders one block of item"
  [{:keys [price name provider_name link image] :as provider}]
  [:div {:class "item"}

   [:div {:class "item-images-container"}
    [:div {:class "item-image"} [:img {:src image}]]]

   [:div {:class "item-main-container"}
    [:div {:class "item-main-top"}
     [:span {:class "item-name"}
      [:a {:href link
           :target "_blank"
           :class "itemlink"} name]]
     ]
    
    [:div {:class "item-main-bottom"}
      ;; Price
      [:div {:class "item-price"} (price-element price)] 

      ;; Provider
     (provider-element provider)
     (sale-element provider)]
     ]

   ])

(defn- search-container [content]
  [:div {:class "container"}
   (let [{:keys [status data]} (:response content)]
     (cond
       (or (= :success status) (empty? (:query content)))
       [:div
        ;;[:p "SUCCESS"]
        [:div (for [i (get-in data [:response :docs])] (render-item i))]]))])

(defn- provider-element [content]
  [:div {:class "provider-element"}
   [:a {:href (:provider_base_url content)}
    [:img {:src (:provider_icon content)
           :title (:provider_name content)
           :alt (:provider_name content)
           :width (get content :provider_icon_w "70")
           :height (get content :provider_icon_h "34")}]]])





(defn search-input [content]
  (form-to
   [:get (str "/search")]
   [:div 
    [:div {:class "query-container"}
     (text-field
      {:size 50 :rows 1 :class "query-text-field"}
      "query"
      (:query content))]]
    [:div
     [:input {:id "search_submit" :type "submit" :value "Search" :tabindex "-1"}]]))

(defn query-example [name href]
  [:li [:a {:class "amber" :href href} name]])

(defn container [content]
  [:div {:class "search-container"}
   [:div {:class "search-left-panel"}
    [:p {:class "query-examples-title"} "Примеры запросов:"]
    [:ul {:class "query-examples"}
     (query-example "Миниатюры (0.05)" "/search?query=0.05+OR+volume%3A%5B*+TO+0.05%5D")
     (query-example "Glenfiddich 12yo 0.7" "/search?query=glenfiddich+12+0.7")
     (query-example "Односолодовый виски" "/search?query=name%3A+\"single+malt\"+OR+type%3Aодносолодовый")
     (query-example "Бленды" "/search?query=name%3Ablend+OR+type%3Aбленд")
     (query-example "Бурбоны" "/search?query=name%3Abourbon+OR+type%3Abourbon")
     (query-example "Акции" "/search?query=sale%3Atrue")
     (query-example "Во вкусе есть ваниль" "/search?query=description%3Aваниль")
     (query-example "Банан и торф" "/search?query=description%3Aбанан+AND+description%3Aторф")
     (query-example "Морская соль" "/search?query=description%3A\"морская+соль\"")
     (query-example "Страна: Шотландия" "/search?query=country%3Aшотландия")
     (query-example "Страна: США" "/search?query=country%3Aсша")
     (query-example "Цена: до 1000 грн" "/search?query=price%3A%5B*+TO+1000%5D")
     (query-example "Бочковая крепость" "/search?query=name%3A\"cask+strength\"+OR+alcohol%3A%5B55+TO+*%5D")
     (query-example "Только Goodwine" "/search?query=provider_name%3AGoodwine")
     (query-example "Бюджетные молты" "/search?query=%28name%3A+\"single+malt\"+OR+type%3Aодносолодовый%29+AND+%28price%3A%5B*+TO+1500%5D%29+AND+0.7")
     ]
    ]
   [:div {:class "search-right-panel"}
    (search-input content)
    (status-bar content)
    (search-container content)
    ]
   ]

  )

(defn- header [content]
  [:div {:class "header"}

   ;; header icon
   [:div {:class "header_icon"}
    [:a {:href "/search" :class "amber h100"}
     [:div 
      [:img {:src "/images/glencairn.png" :width "30" :height "46"}]
      ]
     ]] ;; TODO: Icon an reference to the /search

   
   ;; link to help page
   #_[:div {:class "header_link"}
    [:a {:href "/help" :class "amber h100"} "Помощь"]
    [:a {:href "/stats" :class "amber h100"} "Статистика"]]

   ])

(defn- footer [content]
  (let [version     (get-in content [:meta :version]    "0.0.1")
        build-date  (get-in content [:meta :build-date] (u/now))
        site-name   (get-in content [:meta :name]       "Whisky Search")]
    [:div {:class "footer"}
     [:div {:class "footer-left"}
      [:div {:class "footer-name"} site-name]
      [:div {:class "footer-copyright"} "©"]
      [:div {:class "footer-author"}
       [:a {:href "http://mishadoff.com"
            :class "amber"
            :target "_blank"} "mishadoff.com"]]]

     [:div {:class "footer-center"}]

     [:div {:class "footer-right"
            :align "right"}
      [:div {:class "footer-right-content"}
       (format "Version %s" version)]]
     ]))






(defn search
  "Basic template for searching"
  [content]
  (page/html5
   [:head 
    ;; TODO: provide icon
    [:link {:rel "shortcut icon" :href "/images/favicon.ico"}]
    ;; resources
    (page/include-css "/css/priceous.css")
    (page/include-css "https://fonts.googleapis.com/css?family='Noto+Sans'")]
   [:body 
    (header content)
    (search-container content)
    (footer content)]))

(defn search-new [content]
  (page/html5
   [:head
    ;; TODO: images
    [:link {:rel "shortcut icon" :href "/images/favicon.ico"}]
    (page/include-css "/css/priceous.css")
    (page/include-css "https://fonts.googleapis.com/css?family='Noto+Sans'")]
   [:body
    [:div {:id "main"} 
     
     ;; header
     (header content) 

     ;; main container
     (container content)

     ;; footer
     (footer content)
     ]
    ]))
