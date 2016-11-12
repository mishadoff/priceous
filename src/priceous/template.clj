(ns priceous.template
  (:require [hiccup.core :refer :all]
            [hiccup.form :refer :all]
            [priceous.utils :as u]
            [taoensso.timbre :as log]
            [clj-time.format :as tf]
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
  [{:keys [price name link image] :as provider}]
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
   [:a {:href (:base_url content)}
    [:img {:src (:icon_url content)
           :title (:provider content)
           :alt (:proivder content)
           :width (get content :icon_url_width "70")
           :height (get content :icon_url_height "34")}]]])

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
  [:div {:class "full-container"}
   [:div {:class "search-left-panel"}
    [:p {:class "query-examples-title"} "Примеры запросов:"]
    [:ul {:class "query-examples"}
     (query-example "Миниатюры (0.05)" "/search?query=0.05")
     (query-example "Glenfiddich 12yo 0.7" "/search?query=glenfiddich+12+0.7")
     (query-example "Односолодовый виски" "/search?query=односолодовый")
     (query-example "Бленды" "/search?query=бленд")
     (query-example "Бурбоны" "/search?query=бурбон")
     (query-example "Страна: Шотландия" "/search?query=шотландия")
     (query-example "Только Goodwine" "/search?query=Goodwine")
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
     [:div {:class "header_whisky_block"}
      [:div {:class "header_whisky_block_text"} [:div "Whisky"] [:div "Search"]]
      [:div {:class "header_whisky_block_image"}
       [:img {:src "/images/glencairn.png" :width "30" :height "46"}]]
      ]
     ]]

   
   ;; link to help page
   [:div {:class "header_link"}
    [:a {:href "/help" :class "amber h100"} "Помощь"]]
   [:div {:class "header_link"}
    [:a {:href "/stats" :class "amber h100"} "Статистика"]]]

   )

(defn- footer [content]
  (let [version     (get-in content [:meta :version]    "0.0.2")
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

(defn stats-container [content]
  [:div {:class "full-container"}
   (let [{:keys [status response]} (:response content)]
     (cond
       (not= :success status) [:div "Статистика недоступна."]
       :else
       [:div
        [:ul {:class "help-list"}

         ;; Gather time
         [:li
          [:div (format "Последняя сборка данных была в %s"
                        (tf/unparse (tf/formatters :date-time-no-ms)
                                    (get-in response [:last-gather-ts])))]]
         
         ;; Totals
         [:li 
          [:div (format "В базе всего %s элементов" (get-in response [:total]))]]
         
         ;; Providers
         [:li
          [:div (format "Доступна информация по %s магазинам"
                        (count (get-in response [:providers])))]
          [:ul
           (for [p (get-in response [:providers])]
             [:li (format "%s: всего %s, в наличии есть %s"
                          (:name p) (:total p) (:available p))])]
          ]


         ]]
       
       ))])

(defn help-container [content]
  [:div {:class "full-container"}
   [:div
    [:ul {:class "help-list"}
     [:li 
      [:div
       "Просто введите виски, который вы ищете в строку поиска, например: "
       [:a {:href "/search?query=springbank" :class "amber"} "springbank"] ","
       [:a {:href "/search?query=glenfiddich" :class "amber"} "glenfiddich"] ","
       [:a {:href "/search?query=jameson" :class "amber"} "jameson"]]]
     [:li
      [:div
       "Не нужно никаких кавычек, если это два и больше слов, например: "
       [:a {:href "/search?query=tullamore+dew" :class "amber"} "tullamore dew"] ","
       [:a {:href "/search?query=springbank" :class "amber"} "balvenie sherry"] ","
       [:a {:href "/search?query=clynelish+marsala+finish" :class "amber"} "clynelish marsala finish"]
       ]]
     [:li
      [:div
       "Можно уточнять свой запрос, добавляя выдержку и обьем бутылки, например: "
       [:a {:href "/search?query=glenfiddich+12+0.7" :class "amber"} "glenfiddich 12 0.7"]]]
     [:li
      [:div
       "Также можно уточнять крепость, например: "
       [:a {:href "/search?query=caol+ila+52.8%25" :class "amber"} "caol ila 52.8%"]]]
     [:li
      [:div
       "Можно искать по странам, например:  "
       [:a {:href "/search?query=шотландия" :class "amber"} "шотландия"] ","
       [:a {:href "/search?query=США" :class "amber"} "США"] ","
       [:a {:href "/search?query=тайвань" :class "amber"} "тайвань"]]]
     [:li
      [:div
       "Можно искать по производителям, например:  "
       [:a {:href "/search?query=maltbarn" :class "amber"} "maltbarn"] ","
       [:a {:href "/search?query=gordon+%26+macphail" :class "amber"} "gordon & macphail"]]]
     [:li
      [:div
       "Можно даже искать по категории напитка, например: "
       [:a {:href "/search?query=бленд" :class "amber"} "бленд"] ","
       [:a {:href "/search?query=бурбон" :class "amber"} "бурбон"]]]
     [:li
      [:div
       "Если вы забыли правильное название виски, пишите как слышите, а мы попробуем угадать: "
       [:a {:href "/search?query=гленфидик" :class "amber"} "гленфидик"] ","
       [:a {:href "/search?query=туламор+дью" :class "amber"} "туламор дью"]]]

     [:li
      [:div
       "Всегда показывается только первых 50 результатов, отсортированые от дешевых к дорогим и которые обязательно есть в наличии."]]

     [:li
      [:div
       "Для более продвинутого поиска используйте специальный синтаксис, начинающийся с символа !"]]

     [:li
      [:div
       "Например, поиск только по названию: "
       [:a {:href "/search?query=%21name%3Abourbon" :class "amber"} "! name:bourbon"]]]

     [:li
      [:div
       "Поиск виски в определенном ценовом диапазоне: "
       [:a {:href "/search?query=%21+price%3A%5B*+TO+200%5D" :class "amber"} "! price:[* TO 200]"] ","
       [:a {:href "/search?query=%21+price%3A%5B400+TO+800%5D" :class "amber"} "! price:[400 TO 800]"] ","
       [:a {:href "/search?query=%21+price%3A%5B10000+TO+*%5D" :class "amber"} "! price:[10000 TO *]"]]]

     [:li
      [:div
       "Поиск по крепости: "
       [:a {:href "/search?query=%21+alcohol%3A%5B60+TO+*%5D" :class "amber"} "! alcohol:[60 TO *]"]]]

     [:li
      [:div
       "Можно даже попробовать поискать виски с определенным ароматом или вкусом, например:"
       [:a {:href "/search?query=%21+description%3Aбанан" :class "amber"} "! description:банан"]]]

     [:li
      [:div
       "Все запросы можно комбинировать для получения очень четких запросов, например: "
       [:a {:href "/search?query=%21type%3Aодносолодовый+AND+volume%3A0.7+AND+price%3A%5B*+TO+1500%5D+AND+alcohol%3A%5B45+TO+*%5D+AND+%28description%3Aторф+OR+description%3Aторфяной+OR+description%3Aторфом%29" :class "amber"} "Торфяные односолодовые виски, в бюджете до 1500грн и некомерческой крепостью (45% и више)"]]]
     
     ]]])

(defn help [content]
  (page/html5
   [:head
    [:link {:rel "shortcut icon" :href "/images/favicon.ico"}]
    (page/include-css "/css/priceous.css")
    (page/include-css "https://fonts.googleapis.com/css?family='Noto+Sans'")]
   [:body
    [:div {:id "main"} 
     (header content) 
     (help-container content)
     (footer content)]]))


(defn stats [content]
  (page/html5
   [:head
    ;; TODO: images
    [:link {:rel "shortcut icon" :href "/images/favicon.ico"}]
    (page/include-css "/css/priceous.css")
    (page/include-css "https://fonts.googleapis.com/css?family='Noto+Sans'")]
   [:body
    [:div {:id "main"} 
     (header content) 
     (stats-container content)
     (footer content)]]))
