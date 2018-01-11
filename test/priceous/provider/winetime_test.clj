(ns priceous.provider.winetime-test
  (:require [priceous.provider.winetime :as wt]
            [priceous.common-test :as ct]
            [clojure.test :refer :all]
            [net.cgrand.enlive-html :as enlive]
            [hiccup.core :as hiccup]
            [priceous.utils :as u]))

(deftest test--get-categories
  (ct/testing-categories (wt/get-categories wt/provider)))

(deftest test--node->document-edge-cases
  (testing "Empty page"
    (is (nil? (wt/node->document wt/provider nil))))
  )

(deftest test--node->document-happy-path
  (testing "Some page with all properties present"
    (doseq [[test-in test-out test-meta?] (ct/load-cases "test/resources/winetime")]
      (is (= (u/read-edn test-out)
             (ct/provider-doc wt/node->document (ct/apply-meta wt/provider test-meta?) test-in))))
    ))

(comment
  (ct/save "http://winetime.com.ua/viski-william-grant-and-sons-glenfiddich-12r-tub_200709.htm"
           "test/resources/winetime/CASE_001_glenfiddich_in.edn")
  (ct/save "http://winetime.com.ua/vino-santa-ana-sauvignon-blanc-bile-cuhe_201636.htm"
           "test/resources/winetime/CASE_002_santana_in.edn")

  )