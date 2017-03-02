(ns priceous.templates.query-examples
  (:require [hiccup.core :refer :all]
            [hiccup.page :as page]))

(def queries
  [["Односолодовый виски" "/search?query=односолодовый+виски"]
   ["Шотландские бленды" "/search?query=виски+бленд+шотландия"]
   ["Бурбоны" "/search?query=бурбон"]
   ["Glenfiddich 12yo 0.7" "/search?query=glenfiddich+12+0.7"]
   ["Springbank 10" "/search?query=springbank+10"]
   ["Виски от 50% и выше" "/search?query=%21виски+AND+alcohol%3A%5B50+TO+*%5D"]
   ["Кубинские ромы" "/search?query=ром+куба"]
   ["Текила до 300 грн" "/search?query=%21текила+price:[*+TO+300]"] 
   ["Красное сухое вино" "/search?query=красное+сухое+вино"]
   ["Белое полусладкое" "/search?query=вино+белое+полусладкое"]
   ["Французские вина" "/search?query=вино+франция"]
   ["Винтажи 1989 года" "/search?query=%21вино+AND+vintage%3A1989"]
   ["Шато Мутон" "/search?query=chateau+mouton"]
   ["Шампанское и игристое" "/search?query=%21шампанское+OR+игристое"]
   ["Сорт пино нуар" "/search?query=%21wine_grape%3Aпино%2Bнуар"]
   ["Вино с сахаром от 10 до 20 г/л" "/search?query=%21вино+AND+wine_sugar%3A%5B10+TO+20%5D"]
   ["В аромате банан" "/search?query=%21description%3Aбанан"]
   ["Темное пиво" "/search?query=темное+пиво"]
   ["Водка 0.5" "/search?query=водка+0.5"]
   ["Только Goodwine" "/search?query=Goodwine"]
   ["Новинки" "/search?query=%21item_new%3Atrue"]
   ["Акции" "/search?query=%21sale%3Atrue"]
   ["Наборы с бокалами" "/search?query=%21стакан+OR+стакана+OR+стаканами+OR+бокал+OR+бокалами+OR+бокалы"]
   ["Торфяной виски" "/search?query=%21description%3Aторф+OR+description%3Aторфяной"]
   ["Большие бутылки (от 3л)" "/search?query=%21volume%3A%5B3+TO+*%5D"]
   ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn query-example
  ([name] (query-example name (format "/search?query=%s"
                                      (java.net.URLEncoder/encode name))))
  ([name href] [:a {:class "link" :href href} name]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
