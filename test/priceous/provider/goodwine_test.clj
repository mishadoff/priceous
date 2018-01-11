(ns priceous.provider.goodwine-test
  (:require [priceous.provider.goodwine :as gw]
            [priceous.common-test :as ct]
            [clojure.test :refer :all]
            [net.cgrand.enlive-html :as enlive]
            [hiccup.core :as hiccup]
            [priceous.utils :as u]))

(deftest test--get-categories
  (ct/testing-categories (gw/get-categories gw/provider)))

(deftest test--node->document-edge-cases

  (testing "Empty page"
    (is (nil? (gw/node->document gw/provider nil))))

  (testing "No processing when .mainInfoBlock is unavailable"
    (is (nil? (gw/node->document
                gw/provider
                {:page (enlive/html-snippet "<div class='_mainInfoProd'>something</div>")
                 :node nil :link nil}))))
  )

(deftest test--node->document-happy-path
  (testing "Some page with all properties present"
    (doseq [[test-in test-out test-meta?] (ct/load-cases "test/resources/goodwine")]
      (is (= (u/read-edn test-out)
             (ct/provider-doc gw/node->document
                              (ct/apply-meta gw/provider test-meta?)
                              test-in))))
    ))

(comment
  (ct/save "http://goodwine.com.ua/glenfiddich-12-yo-tube-01347.html"
        "test/resources/goodwine/CASE_001_glenfiddich_in.edn")
  (ct/save "http://goodwine.com.ua/sauvignon-blanc-marlborough-sun-05776.html"
        "test/resources/goodwine/CASE_002_marlborough_in.edn")

  )