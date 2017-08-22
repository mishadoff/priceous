(ns priceous.utils-test
  (:require [priceous.utils :refer :all]
            [clojure.test :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [priceous.utils :as u]))

;; make sure debug does not corrupt messages
(defspec debug--equal-property-test
  (prop/for-all [e gen/simple-type] (= e (debug e))))

(deftest debug-lens-test
  (is (= 1 (debug-lens 1 inc))))

(defspec now--looks-like-a-valid-string
  (prop/for-all [e (gen/elements [(now)])]
                (and (string? e) (= 20 (count e))
                     (.contains e "T") (.contains e "Z"))))

(deftest die-test
  (is (thrown? IllegalArgumentException (die "ExpectedException"))))

(deftest to-date-test
  (testing "Conversion works"
    (is (= "2016-06-08T12:26:17Z" (to-date 1465388777222)))))

(deftest fetch-test
  (testing "Hopefully google.com is not down"
    (is (= 2 (count (fetch "http://google.com")))))
  (testing "Malformed URL"
    (is (nil? (fetch "blablabla"))))
  (testing "UnknownHost URL"
    ;; if some-one register such domain, comment the test.
    (is (nil? (fetch "http://clojure-is-the-best-language.bla.bla.com"))))
  (testing "Resource Not Found"
    ;; if some-one register such domain, comment the test.
    (is (nil? (fetch "http://google.com/404")))))

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

(deftest cleanup-test
  (is (= "CLEAN" (cleanup "CLEAN")))
  (is (= "CLEAN" (cleanup "       CLEAN")))
  (is (= "CLEAN" (cleanup "CLEAN       ")))
  (is (= "CLEAN" (cleanup "       CLEAN       ")))
  (is (= "CLE AN" (cleanup " CLE AN "))))

(deftest get-client-ip-test
  (is (= "10.2.1.1" (get-client-ip {:headers {"x-forwarded-for" "10.2.1.1"}})))
  (is (= "10.2.1.1" (get-client-ip {:headers {"x-forwarded-for" "10.2.1.1,10.2.1.2"}})))
  (is (= "10.1.2.3" (get-client-ip {:remote-addr "10.1.2.3"})))
  )

(defspec elapsed-so-far--time-is-greater-than-passed
  5 ;; run only 5 tests so far
  (prop/for-all [pause (gen/choose 10 1000)]
                (do
                  (Thread/sleep pause)
                  (> (elapsed-so-far pause) (/ pause 1000.0)))))

(deftest full-href-test
  (is (= "a/b" (full-href {:info {:base-url "a"}} "b")))
  (is (= "a/b" (full-href {:info {:base-url "a/"}} "b")))
  (is (= "a/b" (full-href {:info {:base-url "a"}} "/b")))
  (is (= "a/b" (full-href {:info {:base-url "a/"}} "/b"))))

(deftest split-price-test
  (is (= [100 "00"] (split-price 100)))
  (is (= [100 "01"] (split-price 100.01)))
  (is (= [200 "37"] (split-price 200.37)))
  (is (= [0 "00"] (split-price 0.00)))
  (is (= [0 "19"] (split-price 0.19)))
  )

(deftest format-decimal-up-to-2-test
  (is (= "100" (format-decimal-up-to-2 100.0)))
  (is (= "100.54" (format-decimal-up-to-2 100.542234)))
  (is (= "1" (format-decimal-up-to-2 0.9999999)))
  (is (= "0.44" (format-decimal-up-to-2 0.4444444)))
  (is (= "0.5" (format-decimal-up-to-2 0.5))))

(deftest force-pos-test
  (is (= 100 (force-pos 100)))
  (is (nil? (force-pos nil)))
  (is (nil? (force-pos 0)))
  (is (nil? (force-pos -100))))

(u/require-all-providers)

(deftest test--find-all-providers
  (is (= #{"alcoland" "alcomag" "alcoparty" "alcostore"
           "alcovegas" "auchan" "barbados" "bestwine"
           "dutyfreeshop" "dutyfreestore" "elitalco"
           "eliteclub" "elitochka" "etelefon" "fozzy"
           "goodwine" "megamarket" "metro" "novus"
           "rozetka" "winetime"} (set (find-all-providers)))))

(deftest test--find-provider-by-name
  (is (not (nil? (resolve-provider-by-name "goodwine"))))
  (is (nil? (resolve-provider-by-name "superstore"))))

(deftest test--readable-time
  ;; parse time, current time
  (is (= "Сегодня" (readable-time "2017-05-11T08:35:46Z" "2017-05-11T12:00:00Z")))
  (is (= "Сегодня" (readable-time "2017-05-11T08:35:46Z" "2017-05-11T23:59:59Z")))
  (is (= "Сегодня" (readable-time "2017-05-11T00:00:00Z" "2017-05-11T23:59:59Z")))
  (is (= "Вчера" (readable-time "2017-05-11T23:59:59Z" "2017-05-12T00:00:00Z")))
  (is (= "Вчера" (readable-time "2017-05-11T00:00:00Z" "2017-05-12T00:00:00Z")))
  (is (= "Давно" (readable-time "2016-05-11T00:00:00Z" "2017-05-12T00:00:00Z")))
  )

(deftest test--catenate-items
  (is (= "1" (cat-items "1")))
  (is (= "1 2" (cat-items "1" 2)))
  (is (= "1 2" (cat-items "1" "2")))
  (is (= "1 2 3 4 6" (cat-items "1" "2" nil "3" 4 nil " " 6)))
  (is (= "Whisky Bourbon" (cat-items "Whisky" nil nil "Bourbon " nil nil)))
  )

(deftest test--deep-merge
  (is (= nil (deep-merge nil nil)))
  (is (= {:a 2 :b {:c 23 :d "Hey" :e "Hoy"} :f 1 :g 2}
         (deep-merge {:a 1 :b {:c 10 :d "Hey"} :f 1}
                     {:a 2 :b {:c 23 :e "Hoy"} :g 2})))

  )