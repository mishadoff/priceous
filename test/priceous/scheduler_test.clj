(ns priceous.scheduler-test
  (:require [priceous.spider.scheduler :refer :all]
            [clojure.test :refer :all])
  (:import [java.util.concurrent TimeUnit]))

(deftest test-scheduler-default-1-sec
  (let [counter (atom 0)]
    ;; increment counter every second
    (schedule-submit-function #(swap! counter inc))
    ;; wait slightly more than 2 seconds
    (Thread/sleep 2100)
    (schedule-cancel)
    (is (= @counter 3))))

(deftest test-scheduler-default-3-sec
  (let [counter (atom 0)]
    ;; increment counter every second
    (schedule-submit-function #(swap! counter inc) :value 3)
    ;; wait slightly more than 2 seconds
    (Thread/sleep 2100)
    (schedule-cancel)
    (is (= @counter 1))))

(deftest test-scheduler-delayed-1-sec
  (let [counter (atom 0)]
    ;; increment counter every second
    (schedule-submit-function #(swap! counter inc) :delay 3)
    ;; wait slightly more than 2 seconds
    (Thread/sleep 2100)
    (schedule-cancel)
    (is (= @counter 0))))

(deftest test-reschedule
  (let [counter (atom 0)]
    ;; increment counter every second
    (schedule-submit-function #(swap! counter inc))
    ;; wait slightly more than 2 seconds
    (Thread/sleep 2100)
    (is (= @counter 3))
    ;; reschedule
    (schedule-submit-function #(swap! counter inc) :value 100 :time-unit TimeUnit/MILLISECONDS)
    (Thread/sleep 950)
    (is (= @counter 13))
    (schedule-cancel)))
