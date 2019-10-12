(ns priceous.web.templates.query-examples
  (:require [hiccup.core :refer :all]
            [hiccup.page :as page]))

(def queries
  [["Односолодовый виски" "/search?query=односолодовый+виски"]
   ["Шотландские бленды" "/search?query=виски+бленд+шотландия"]
   ["Бурбоны" "/search?query=бурбон"]
   ["Glenfiddich 12yo 0.7" "/search?query=glenfiddich+12+0.7"]
   ["Springbank 10" "/search?query=springbank+10"]
   ["Виски от 50% и выше" "/search?query=виски+крепость+от+50"]
   ["Кубинские ромы" "/search?query=ром+куба"]
   ["Текила до 300 грн" "/search?query=текила+цена+от+100+до+300"] 
   ["Красное сухое вино" "/search?query=красное+сухое+вино"]
   ["Белое полусладкое" "/search?query=вино+белое+полусладкое"]
   ["Французские вина" "/search?query=вино+франция+цена+от+100"]
   ["Винтажи 1989 года" "/search?query=вино+1989"]
   ["Шато Мутон" "/search?query=chateau+mouton"]
   ["Игристое" "/search?query=игристое"]
   ["Сорт пино нуар" "/search?query=пино+нуар"]
   ["Вино с сахаром от 10 до 20 г/л" "/search?query=вино+сахар+от+10+до+20"]
   #_["В аромате банан" "/search?query=%21description%3Aбанан"]
   ["Темное пиво" "/search?query=темное+пиво"]
   ["Водка 0.5" "/search?query=водка+0.5"]
   ["Только Goodwine" "/search?query=Goodwine"]
   ["Новинки" "/search?query=новинки"]
   ["Акции" "/search?query=акции"]
   ["Виски с бокалами" "/search?query=виски+с+бокалами"]
   #_["Торфяной виски" "/search?query=%21description%3Aторф+OR+description%3Aторфяной"]
   ["Большие бутылки (от 3л до 10л)" "/search?query=обьем+от+3+до+10"]])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn query-example
  ([name] (query-example name (format "/search?query=%s"
                                      (java.net.URLEncoder/encode name))))
  ([name href] [:a {:class "link" :href href} name]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
