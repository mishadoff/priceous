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
       [:a {:href "/search?query=springbank" :class "amber"} "springbank"] ","
       [:a {:href "/search?query=glenfiddich" :class "amber"} "glenfiddich"] ","
       [:a {:href "/search?query=jameson" :class "amber"} "jameson"]]]
     [:li
      [:div
       "Не нужно никаких кавычек, если это два и больше слов, например: "
       [:a {:href "/search?query=tullamore+dew" :class "amber"} "tullamore dew"] ","
       [:a {:href "/search?query=balvenie+sherry" :class "amber"} "balvenie sherry"] ","
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
       "Всегда показывается только первых 50 результатов, отсортированные от дешевых к дорогим и которые обязательно есть в наличии."]]

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
