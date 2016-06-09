(ns priceous.selector-utils-test
  (:require [priceous.selector-utils :refer :all]
            [clojure.test :refer :all]))

;; FIXME: poor coverage
(deftest retrieve-test
  (let [provider "mock-provider"
        node-mock-one [1] node-mock-empty [] node-mock-multiple [1 2]]

    (testing "Provider and Selector are required"

      (is (thrown? AssertionError (retrieve node-mock-one)))
      (is (thrown? AssertionError (retrieve node-mock-one :provider {})))
      (is (thrown? AssertionError (retrieve node-mock-one :selector [:.test-div-1]))))

    (testing "One Element"
      (is (= {:result 1 :status :success}
             (retrieve node-mock-one
                       :provider provider
                       :selector [:.test-div-1]
                       :required true :count-strategy :single)))
      (is (= {:result 1 :status :success}
             (retrieve node-mock-one
                       :provider provider
                       :selector [:.test-div-1])))
      (is (= {:result nil :status :error}
             (-> (retrieve node-mock-empty
                           :provider provider
                           :selector [:.test-div-1])
                 (select-keys [:result :status]))))
      (is (= {:result nil :status :success}
             (-> (retrieve node-mock-empty
                           :provider provider
                           :required false
                           :selector [:.test-div-1])
                 (select-keys [:result :status]))))
      (is (= {:result 1 :status :warn}
             (-> (retrieve node-mock-multiple
                           :provider provider
                           :selector [:.test-div-1])
                 (select-keys [:result :status]))))
      (is (= {:result 1 :status :warn}
             (-> (retrieve node-mock-multiple
                           :required false
                           :provider provider
                           :selector [:.test-div-1])
                 (select-keys [:result :status])))))
    
    (testing "Multiple elements"
      (is (= {:result [1] :status :success}
             (retrieve node-mock-one
                       :provider provider
                       :selector [:.test-div-1]
                       :required true :count-strategy :multiple)))
      (is (= {:result [1 2] :status :success}
             (retrieve node-mock-multiple
                       :provider provider
                       :selector [:.test-div-1]
                       :required true :count-strategy :multiple)))
      (is (= {:result nil :status :error}
             (-> (retrieve node-mock-empty
                           :provider provider
                           :selector [:.test-div-1]
                           :required true :count-strategy :multiple)
                 (select-keys [:result :status]))))
      (is (= {:result nil :status :success}
             (-> (retrieve node-mock-empty
                           :provider provider
                           :selector [:.test-div-1]
                           :required false :count-strategy :multiple)
                 (select-keys [:result :status]))))
      )))

(deftest build-spec-map-test
  (testing "Specs by class"
    (let [page {:tag :div :attrs {}
                :content  '({:tag :div :attrs {:class "spec-key"}
                             :content ({:tag :span :attrs {} :content ("key1")})}
                            {:tag :div :attrs {:class "spec-key"}
                             :content ({:tag :span :attrs {} :content ("key2")})}
                            {:tag :div :attrs {:class "spec-value"}
                             :content ({:tag :span :attrs {} :content ("val1")})}
                            {:tag :div :attrs {:class "spec-value"}
                             :content ({:tag :span :attrs {} :content ("val2")})})}
          spec-map (build-spec-map {:info {:name "test-provider"}}
                                   page
                                   [:.spec-key]
                                   [:.spec-value])]
      (is (= (spec-map "key1") "val1"))
      (is (= (spec-map "key2") "val2"))
      (is (= 2 (count spec-map)))
      ))

  ;; TODO: spec by tags
  ;; TODO: spec by order
  ;; TODO: spec by table
  ;; TODO: non uniform maps
  
  )

(deftest generic-next-page?-test
  (let [page {:tag :div :attrs {}
              :content  '({:tag :div :attrs {:class "last-page"}
                           :content ({:tag :span :attrs {} :content ("34")})})}
        provider {:info {:name "test-provider"}}
        selector [:.last-page :span]
        fn-for-page (fn [page-number]
                      ((generic-next-page? selector)
                       (assoc-in provider [:state :page-current] page-number)
                       page))]
    (is (true? (fn-for-page 1)))
    (is (true? (fn-for-page 33)))
    (is (false? (fn-for-page 34)))
    (is (false? (fn-for-page 35)))))

(deftest generic-page-urls-test
  (let [page {:tag :div :attrs {}
              :content  '({:tag :div :attrs {:class "some-urls"}
                           :content ({:tag :a :attrs {:href "http://a.com/1"} :content ()}
                                     {:tag :a :attrs {:href "http://a.com/2"} :content ()}
                                     {:tag :a :attrs {:href "http://a.com/3"} :content ()})})}
        provider {:info {:name "test-provider"}}
        urls ((generic-page-urls [:.some-urls :a]) provider page)]
    (is (= ["http://a.com/1" "http://a.com/2" "http://a.com/3"] urls))))


(deftest generic-page-urls-with-prefix-test
  (let [page {:tag :div :attrs {}
              :content  '({:tag :div :attrs {:class "some-urls"}
                           :content ({:tag :a :attrs {:href "/1"} :content ()}
                                     {:tag :a :attrs {:href "/2"} :content ()}
                                     {:tag :a :attrs {:href "/3"} :content ()})})}
        provider {:info {:name "test-provider"}}
        urls ((generic-page-urls-with-prefix [:.some-urls :a] "http://a.com") provider page)]
    (is (= ["http://a.com/1" "http://a.com/2" "http://a.com/3"] urls))))
