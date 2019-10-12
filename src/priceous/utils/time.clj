(ns priceous.utils.time
  (:require [clj-time.format :as tf]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]))

;;;

(defn readable [ts-string current-ts-string]
  (let [jt (tf/parse (tf/formatters :date-time-no-ms) ts-string)
        jt-now (tf/parse (tf/formatters :date-time-no-ms) current-ts-string)
        mid (fn [dt] (t/date-midnight (t/year dt) (t/month dt) (t/day dt)))
        jt-mid (mid jt) jt-now (mid jt-now)
        diff-in-days (t/in-days (t/interval jt-mid jt-now))]
    (cond
      (= diff-in-days 0) "Сегодня"
      (= diff-in-days 1) "Вчера"
      (> diff-in-days 1) "Давно"
      :else nil)))

;;;

(defn elapsed-so-far [start]
  (/ (- (System/currentTimeMillis) start) 1000.0))

;;;


(defn now-dt [] (tc/from-long (System/currentTimeMillis)))

(defn to-date [unix-time]
  (->> (tc/from-long unix-time)
       (tf/unparse (tf/formatters :date-time-no-ms))))

(defn to-date-from-string [s]
  (tf/parse (tf/formatters :date-time-no-ms) s))

(defn now [] (to-date (System/currentTimeMillis)))