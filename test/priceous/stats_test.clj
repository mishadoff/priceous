(ns priceous.stats-test
  (:require [priceous.stats :refer :all]
            [clojure.test :refer :all]))

(deftest test--data-coverage
   (testing "Happy cases"
     (is (= 0.1149425287356322 (data-coverage {:name "A"})))
     (is (= 0.2758620689655172 (data-coverage {:name "A" :price 10.0 :volume 0.7}))))

   (testing "Edge cases"
     (is (= 0.0 (data-coverage {})))
     (is (= 0.0 (data-coverage nil)))
     (is (= 1.0 (data-coverage weights))))

   (testing "Practical use cases"
     (is (= 0.8505747126436782
            (data-coverage {:provider         "Goodwine"
                            :name             "Виски Springbank 10yo (0,7л)"
                            :link             "http://somelink"
                            :image            "imglink"
                            :country          "Шотландия"
                            :wine_sugar       nil
                            :wine_grape       nil
                            :vintage          nil
                            :producer         "Springbank Inc."
                            :type             "Крепкие Виски"
                            :alcohol          43.7
                            :description      "Слегка торфяной с нотками цитрусовых"
                            :product-code     "Goodwine_12345"
                            :available        true
                            :item_new         false
                            :volume           0.7
                            :price            1234.0
                            :sale             false
                            :sale-description nil
                            :excise           true
                            :trusted          true
                            }))))

   (testing "Avg for all documents"
     (is (= 0.0 (data-coverage-avg [])))
     (is (= 0.0 (data-coverage-avg nil)))
     (is (= 0.1149425287356322 (data-coverage-avg [{:name "A"}])))
     (is (= 0.1149425287356322 (data-coverage-avg [{:name "A"} {:name "B"} {:name "C"}])))
     (is (= 0.09195402298850575 (data-coverage-avg [{:name "A"} {:price 1.0} {:volume 0.7}]))))

     )