(ns priceous.utils
  (:require [clj-time.coerce :as tc]
            [clj-time.format :as tf]
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
 trim-inside
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

(defn smart-parse-double [s]
  (some-> s
          ;; replace commas to periods
          (clojure.string/replace "," ".")
          ;; remove all alien symbols
          (clojure.string/replace #"[^0-9\\.]+" "")
          ;; swap empty string with nils to be handled by some->
          ((fn [s] (if (empty? s) nil s))) 
          ;; if it is still not valid string
          ((fn [s]
             (try (Double/parseDouble s)
                  (catch NumberFormatException e
                    (log/error "Can't parse value " s)
                    nil))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; TODO fetch witth retry
;; TODO fetch with timeout
(defn fetch [url]
  (log/trace "Fetching URL: " url)
  (try 
    (html/html-resource (java.net.URL. url))
    (catch Exception e (log/error e) nil)))


(defn cleanup
  "Remove whitespace charaters from string"
  [s]
  (some-> s
   (.replaceAll "\\s+" " ")
   (clojure.string/trim)))

(defn falsy []
  "Returns function which accepts any number of arguments
   and always return false"
  (fn [& _] false))

(defn truthy []
  "Returns function which accepts any number of arguments
   and always return true"
  (fn [& _] true))

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

(defn require-all-providers []
  ;; Require all namespaces in priceous.provider.* folder
  (let [symbols (->> (cns/find-namespaces-on-classpath)
                     (filter (fn [sym] (clojure.string/starts-with? (str sym) "priceous.provider."))))]
    (doseq [provider-ns symbols]
      (require provider-ns))))


(defn split-price [price]
  (let [grn (bigint (Math/floor price))
        kop (->> (int (* 100 (- price grn)))
                 (format "%2d")
                 ((fn [s] (clojure.string/replace s " " "0"))))]
    [grn kop]))
