(ns priceous.utils-test
  (:require [priceous.utils :refer :all]
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

      (is (= {:result nil :status :warn}
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

      (is (= {:result nil :status :warn}
             (-> (retrieve node-mock-empty
                           :provider provider
                           :selector [:.test-div-1]
                           :required false :count-strategy :multiple)
                 (select-keys [:result :status]))))

      )))

(deftest smart-parse-double-test
  ;; happy cases
  (is (= 1.0 (smart-parse-double "1")))
  (is (= 1235.7 (smart-parse-double "1235.7")))
  (is (= 12.04 (smart-parse-double "12.04")))

  ;; handle commas gracefully
  (is (= 1.7 (smart-parse-double "1,7")))
  (is (= 123.75 (smart-parse-double "123,75")))

  ;; remove intruders
  (is (= 1.7 (smart-parse-double "1,7л")))
  (is (= 1.7 (smart-parse-double "1.7 литров")))
  (is (= 1.7 (smart-parse-double "   1 , 7  ")))
  (is (= 1.7 (smart-parse-double "  1.7л")))

  ;; empty strings
  (is (nil? (smart-parse-double nil)))
  (is (nil? (smart-parse-double "")))

  ;; other edge cases
  (is (= 0.0 (smart-parse-double "0000")))
  (is (nil? (smart-parse-double "..")))
  
  )



