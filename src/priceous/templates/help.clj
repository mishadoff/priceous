(ns priceous.templates.help
  (:require [hiccup.core :refer :all]
            [priceous.templates.base :as base]))

(declare view help-container)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn view [response] (base/page help-container response))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn help-container [content]
  [:div {:class "full-container"}
   [:div
    [:ul {:class "help-list"}
     [:li 
      [:div
       "Просто введите название виски, который вы ищете в строку поиска, например: "
       [:a {:href "/search?query=springbank" :class "link"} "springbank"] ","
       [:a {:href "/search?query=glenfiddich" :class "link"} "glenfiddich"] ","
       [:a {:href "/search?query=jameson" :class "link"} "jameson"]]]
     [:li
      [:div
       "Не нужно никаких кавычек, если это два и больше слов, например: "
       [:a {:href "/search?query=tullamore+dew" :class "link"} "tullamore dew"] ","
       [:a {:href "/search?query=balvenie+sherry" :class "link"} "balvenie sherry"] ","
       [:a {:href "/search?query=clynelish+marsala+finish" :class "link"} "clynelish marsala finish"]
       ]]
     [:li
      [:div
       "Можно уточнять свой запрос, добавляя выдержку и обьем бутылки, например: "
       [:a {:href "/search?query=glenfiddich+12+0.7" :class "link"} "glenfiddich 12 0.7"]]]
     [:li
      [:div
       "Также можно уточнять крепость, например: "
       [:a {:href "/search?query=caol+ila+52.8%25" :class "link"} "caol ila 52.8%"]]]
     [:li
      [:div
       "Можно искать по странам, например:  "
       [:a {:href "/search?query=шотландия" :class "link"} "шотландия"] ","
       [:a {:href "/search?query=США" :class "link"} "США"] ","
       [:a {:href "/search?query=тайвань" :class "link"} "тайвань"]]]
     [:li
      [:div
       "Можно искать по производителям, например:  "
       [:a {:href "/search?query=maltbarn" :class "link"} "maltbarn"] ","
       [:a {:href "/search?query=gordon+%26+macphail" :class "link"} "gordon & macphail"]]]
     [:li
      [:div
       "Можно даже искать по категории напитка, например: "
       [:a {:href "/search?query=бленд" :class "link"} "бленд"] ","
       [:a {:href "/search?query=бурбон" :class "link"} "бурбон"]]]
     [:li
      [:div
       "Если вы забыли правильное название виски, пишите как слышите, а мы попробуем угадать: "
       [:a {:href "/search?query=гленфидик" :class "link"} "гленфидик"] ","
       [:a {:href "/search?query=туламор+дью" :class "link"} "туламор дью"]]]

     [:li
      [:div
       "Всегда показывается только первых 50 результатов, отсортированные от дешевых к дорогим и которые обязательно есть в наличии."]]

     [:li
      [:div
       "Для более продвинутого поиска используйте специальный синтаксис, начинающийся с символа !"]]

     [:li
      [:div
       "Например, поиск только по названию: "
       [:a {:href "/search?query=%21name%3Abourbon" :class "link"} "! name:bourbon"]]]

     [:li
      [:div
       "Поиск виски в определенном ценовом диапазоне: "
       [:a {:href "/search?query=%21+price%3A%5B*+TO+200%5D" :class "link"} "! price:[* TO 200]"] ","
       [:a {:href "/search?query=%21+price%3A%5B400+TO+800%5D" :class "link"} "! price:[400 TO 800]"] ","
       [:a {:href "/search?query=%21+price%3A%5B10000+TO+*%5D" :class "link"} "! price:[10000 TO *]"]]]

     [:li
      [:div
       "Поиск по крепости: "
       [:a {:href "/search?query=%21+alcohol%3A%5B60+TO+*%5D" :class "link"} "! alcohol:[60 TO *]"]]]

     [:li
      [:div
       "Можно даже попробовать поискать виски с определенным ароматом или вкусом, например:"
       [:a {:href "/search?query=%21+description%3Aбанан" :class "link"} "! description:банан"]]]

     [:li
      [:div
       "Все запросы можно комбинировать для получения очень четких запросов, например: "
       [:a {:href "/search?query=%21type%3Aодносолодовый+AND+volume%3A0.7+AND+price%3A%5B*+TO+1500%5D+AND+alcohol%3A%5B45+TO+*%5D+AND+%28description%3Aторф+OR+description%3Aторфяной+OR+description%3Aторфом%29" :class "link"} "Торфяные односолодовые виски, в бюджете до 1500грн и некомерческой крепостью (45% и више)"]]]
     
     ]]])
