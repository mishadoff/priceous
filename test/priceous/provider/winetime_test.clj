(ns priceous.provider.winetime-test
  (:require [priceous.provider.winetime :as wt]
            [priceous.common-test :as ct]
            [clojure.test :refer :all]
            [net.cgrand.enlive-html :as enlive]
            [hiccup.core :as hiccup]))

(deftest test--get-categories
  (ct/testing-categories (wt/get-categories wt/provider)))

(deftest test--node->document-edge-cases
  (testing "Empty page"
    (is (nil? (wt/node->document wt/provider nil))))
  )

(deftest test--node->document-happy-path
  (testing "Some page with all properties present"
    (is (= {:alcohol      40.0
            :available    true
            :country      "Шотландия Спейсайд"
            :description  "Виски янтарного цвета с отблесками. Виски обладает лёгким, свежим ароматом с тонами цитрусов, груши, дуба, нотками орехов и солода. Виски обладает мягким, округлым, сладковатым вкусом с нотками ванили, груши, мёда и длительным, сухим послевкусием с нюансами шоколада."
            :excise       true
            :image        "http://winetime.com.ua/modules/pages/pictures/371x371/1383136619_2461.jpg"
            :item_new     false
            :link         "http://winetime.com.ua/viski-william-grant-and-sons-glenfiddich-12r-tub_200709.htm"
            :name         "Glenfiddich 12Y.O. (в тубусе)"
            :price        1197.0
            :producer     "Glenfiddich"
            :product-code "Winetime_46574"
            :provider     "Winetime"
            :sale         false
            :trusted      true
            :type         "виски односолодовый"
            :vintage      nil
            :volume       0.7
            :wine_grape   nil
            :wine_sugar   nil}

           (ct/provider-heavy-node-doc
             wt/node->document
             wt/provider
             "http://winetime.com.ua/viski-william-grant-and-sons-glenfiddich-12r-tub_200709.htm")))

    (is (= {:alcohol      13.5
            :available    true
            :country      "Аргентина Мендоза"
            :description  "Аромат травянисто-фруктовый с нотками листа смородины, лайма; Вкус свежий, сбалансированный"
            :excise       true
            :image        "http://winetime.com.ua/modules/pages/pictures/371x371/1461332635_50416.jpg"
            :item_new     false
            :link         "http://winetime.com.ua/vino-santa-ana-sauvignon-blanc-bile-cuhe_201636.htm"
            :name         "Santa Ana Sauvignon Blanc"
            :price        197.0
            :producer     "Santa Ana"
            :product-code "Winetime_58448"
            :provider     "Winetime"
            :sale         false
            :trusted      true
            :type         "Вино белое полусухое"
            :vintage      "2015"
            :volume       0.75
            :wine_grape   "100% совиньон блан"
            :wine_sugar   6.0}

           (ct/provider-heavy-node-doc
             wt/node->document
             (assoc-in wt/provider [:state :category] "Вино")
             "http://winetime.com.ua/vino-santa-ana-sauvignon-blanc-bile-cuhe_201636.htm")))
    ))