(ns priceous.selector-utils-test
  (:require [priceous.selector-utils :refer :all]
            [clojure.test :refer :all]))

(deftest retrieve-tests
  (let [provider "mock-provider"
        node-mock-one [1] node-mock-empty [] node-mock-multiple [1 2]]

    (testing "Provider and Selector are required"

      (is (thrown? AssertionError (retrieve node-mock-one)))

      (is (thrown? AssertionError (retrieve node-mock-one :provider {})))

      (is (thrown? AssertionError (retrieve node-mock-one :selector [:.test-div-1])))

      )

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
                 (select-keys [:result :status]))))
      
      )

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


