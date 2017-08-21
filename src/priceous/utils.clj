(ns priceous.utils
  (:require [clj-time.coerce :as tc]
            [clj-time.format :as tf]
            [clj-time.core :as t]
            [taoensso.timbre :as log]
            [clojure.string :as s]
            [net.cgrand.enlive-html :as html]
            [clojure.tools.namespace :as cns]
            ))

(declare
 die
 debug
 now
 to-date
 smart-parse-double
 fetch
 cleanup
 falsy
 )

;;;;;;;;;;;;;;;;;;;;;;
;; Common Utilities ;;
;;;;;;;;;;;;;;;;;;;;;;

(defn die [message]
  (throw (IllegalArgumentException. message)))

(defn debug [e] (log/debug e) e)

(defn debug-lens [e f]
  (log/debug "\tDEBUGGING LENS\t" (f e))
  e)

(defn now [] (to-date (System/currentTimeMillis)))

(defn now-dt [] (tc/from-long (System/currentTimeMillis)))

(defn to-date [unix-time]
  (->> (tc/from-long unix-time)
       (tf/unparse (tf/formatters :date-time-no-ms))))

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; TODO fetch with retry
;; TODO fetch with timeout
(defn fetch [url]
  (log/trace "Fetching URL: " url)
  (try
    (html/html-resource (java.net.URL. url))
    (catch Exception e (log/error e) nil)))


(defn cleanup
  "Remove whitespace charaters from string
   Empty string becames nil"
  [s]
  (some->
    s
    (.replaceAll "&nbsp;" " ")
    (.replaceAll "\\s+" " ")
    (clojure.string/trim)
    ((fn [s] (if (empty? s) nil s)))))

(defn cat-items
  "Concatenate set of items into space delimited string, nils skipped"
  [& items]
  (->> items
       (remove nil?)
       (interpose " ")
       (apply str)
       (cleanup)))

(defn full-href [provider part-href]
  (let [base-url (get-in provider [:info :base-url])]
    (str
     (if (s/ends-with? base-url "/")
       (subs base-url 0 (dec (count base-url)))
       base-url)
     "/"
     (if (s/starts-with? part-href "/")
       (subs part-href 1)
       part-href))))

(defn get-client-ip [req]
  (if-let [ips (get-in req [:headers "x-forwarded-for"])]
    (-> ips (clojure.string/split #",") first)
    (:remote-addr req)))

(defn elapsed-so-far [start]
  (/ (- (System/currentTimeMillis) start) 1000.0))

(defn resolve-provider-by-name [pname]
  (try (-> (format "priceous.provider.%s/provider" pname)
           symbol
           resolve
           var-get)
       (catch Exception e
         (log/error (format "Can not resolve provider [%s] %s" pname (.getMessage e))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn require-all-providers []
  ;; Require all namespaces in priceous.provider.* folder
  (let [symbols (->> (cns/find-namespaces-on-classpath)
                     (filter (fn [sym] (clojure.string/starts-with? (str sym) "priceous.provider."))))]
    (doseq [provider-ns symbols]
      (require provider-ns))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn find-all-providers []
  (->> (cns/find-namespaces-on-classpath)
       (map str)
       (filter (fn [ns-name] (clojure.string/starts-with? ns-name "priceous.provider.")))
       (filter (fn [ns-name]
                 (not (nil?
                       (some-> (format "%s/provider" ns-name)
                               symbol
                               resolve
                               var-get)))))
       (map (fn [ns-name] (last (seq (.split ns-name "\\.")))))
       (doall)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn split-price [price]
  (let [grn (bigint (Math/floor price))
        kop (->> (int (* 100 (- price grn)))
                 (format "%2d")
                 ((fn [s] (clojure.string/replace s " " "0"))))]
    [grn kop]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn format-decimal-up-to-2 [decimal]
  (let [decimal-fmt (format "%.2f" decimal)]
    (-> (cond
          (.endsWith decimal-fmt "00")
          (.substring decimal-fmt 0 (- (count decimal-fmt) 3))
          (.endsWith decimal-fmt "0")
          (.substring decimal-fmt 0 (- (count decimal-fmt) 1))
          :else decimal-fmt))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn readable-time [ts-string current-ts-string]
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn force-pos [n]
  (if (and n (pos? n)) n nil))
