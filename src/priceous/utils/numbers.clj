(ns priceous.utils.numbers
  (:require [taoensso.timbre :as log]))

;;;

(defn format-decimal-up-to-2 [decimal]
  (let [decimal-fmt (format "%.2f" decimal)]
    (-> (cond
          (.endsWith decimal-fmt "00")
          (.substring decimal-fmt 0 (- (count decimal-fmt) 3))
          (.endsWith decimal-fmt "0")
          (.substring decimal-fmt 0 (- (count decimal-fmt) 1))
          :else decimal-fmt))))

;;;

(defn split-price [price]
  (let [grn (bigint (Math/floor price))
        kop (->> (int (* 100 (- price grn)))
                 (format "%2d")
                 ((fn [s] (clojure.string/replace s " " "0"))))]
    [grn kop]))

;;;

(defn positive-or-nil [n]
  (if (and n (pos? n)) n nil))

;;;

(defn smart-parse-double [st]
  (some-> st
          ;; replace commas to periods
          (clojure.string/replace "," ".")
          ;; drop everything after dash
          ((fn [s]
             (let [dash-index (.indexOf s "-")]
               (if (= dash-index -1) s (subs s 0 dash-index)))))

          ;; remove all alien symbols
          (clojure.string/replace #"[^0-9\\.]+" "")
          ;; swap empty string with nils to be handled by some->
          ((fn [s] (if (empty? s) nil s)))
          ;; if it is still not valid string

          ;; if it contains more than one period drop it
          ((fn [s]
             (let [dots (re-seq #"\." s)]
               (if (< (count dots) 2) s
                                      (let [dot-index (.indexOf s ".")
                                            dot-index-2 (.indexOf s "." (inc dot-index))]
                                        (subs s 0 dot-index-2))))))

          ((fn [s]
             (try (Double/parseDouble s)
                  (catch NumberFormatException e
                    (log/error (format "Can't parse value %s original was %s" s st))
                    nil))))))