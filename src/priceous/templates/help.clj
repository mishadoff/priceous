(ns priceous.templates.help
  (:require [hiccup.core :refer :all]
            [priceous.templates.base :as base]
            [priceous.templates.query-examples :as qe]))

(declare view help-container)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn view [response] (base/page help-container response {:title "Помощь"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn help-container [content]
  [:div {:class "help-container"}
   [:h3 "Простые запросы"]
   [:p
    "Введите, то что вы ищете в строку поиска. "
    "Это может быть как общий запрос ("
    (qe/query-example "виски") ", " (qe/query-example "вино")
    "), так и более конкретный (" 
    (qe/query-example "espolon") ", " (qe/query-example "jameson") ")"
    [:br]
    
    "Не нужно никаких кавычек, если в вашем запросе два или больше слов ("
    (qe/query-example "laphroaig quarter cask") ", "
    (qe/query-example "текила jose cuervo") ")"
    [:br]

    "Можно уточнять запрос указывая обьем, выдержку или крепость продукта ("
    (qe/query-example "glenfiddich 12 лет 0.7") ", "
    (qe/query-example "wild turkey 50.5%") ", "
    (qe/query-example "ром 23 года") ")"
    [:br]
    
    "Также можно уточнять страну, регион и производителя ("
    (qe/query-example "вино франция бордо") ", "
    (qe/query-example "ром ямайка") ", "
    (qe/query-example "виски maltbarn") ")"
    [:br]

    "Если вы не знаете как пишется напиток, пишите как слышите, а мы попробуем угадать ("
    (qe/query-example "лафройг") ", "
    (qe/query-example "гленфидик") ", "
    (qe/query-example "туламор дью") ")"
    [:br]

    "Вообще, все что вас интересует просто пишите в строку поиска, мы разберемся ("
    (qe/query-example "акции") ", "
    (qe/query-example "новинки") ", "
    (qe/query-example "виски с бокалами") ")"
    ]

   [:h3 "Фильтры"]
   [:p
    "Если ваш запрос возвращает много результатов, попробуйте ограничить результаты по цене, крепости или обьему, используя соответствующие ключевые слова \"цена\", \"крепость\" или \"обьем\" и задайте границы фильтра используя слова \"от\" и \"до\" ("
    (qe/query-example "вино цена до 300 грн обьем от 0.75") ", "
    (qe/query-example "ром крепость от 50") ", "
    (qe/query-example "виски цена от 1000 до 2000 грн") ")"
    ;; "Кстати, слово \"цена\" можно не писать ("
    ;; (qe/query-example "виски бленд до 500") ", "
    ;; (qe/query-example "вино от 1000") ", "
    ;; (qe/query-example "игристое от 100 до 300") ")"
    ]

   [:h3 "Вино"]
   [:p
    "Если вы ищете вина, то можно искать по типу ("
    (qe/query-example "вино красное сухое") ", "
    (qe/query-example "вино белое полусладкое") ", "
    (qe/query-example "вино игристое") ")"
    [:br]

    "Также можно добавить винтаж или сорт винограда ("
    (qe/query-example "вино 1984") ", "
    (qe/query-example "каберне фран 1989") ", "
    (qe/query-example "шардоне пино нуар") ")"
    [:br]
    
    "Некоторые магазины показывают сколько сахара в вине (из расчета грам на литр). "
    "Вы можете использовать этот параметр в фильтрах ("
    (qe/query-example "красное вино сахар от 5 до 10") ", "
    (qe/query-example "вино сахар от 150")
    ")"
    

    ]
   ])
