(ns priceous.utils
  (:require [clj-time.coerce :as tc]
            [clj-time.format :as tf]
            [taoensso.timbre :as log]
            [net.cgrand.enlive-html :as html]))

(declare
 die                    ;; TESTED, TODO: move to ex ns
 debug                  ;; TESTED, TODO: move to debug ns
 to-date                ;; TESTED, TODO: move to date ns
 now                    ;; TESTED, TODO: move to date ns
 smart-parse-double     ;; TESTED, TODO: move to number ns
 fetch                  ;; TESTED, TODO: move to http utils
 cleanup                ;; TESTED, TODO: move to str utils
 falsy                  ;; TESTED, TODO: test
 )

;;;;;;;;;;;;;;;;;;;;;;
;; Common Utilities ;;
;;;;;;;;;;;;;;;;;;;;;;

(defn die [message]
  {:pre [message]}
  (throw (IllegalArgumentException. message)))

(defn debug [e] (log/debug e) e)

(defn to-date [unix-time]
  (->> (tc/from-long unix-time)
       (tf/unparse (tf/formatters :date-time-no-ms))))

(defn now [] (to-date (System/currentTimeMillis)))
(defn now-dt [] (tc/from-long (System/currentTimeMillis)))

(defn smart-parse-double [s]
  (some-> s
          ;; repalce commas to periods
          (clojure.string/replace "," ".")
          ;; remove all alienm symbols
          (clojure.string/replace #"[^0-9\\.]+" "")
          ;; swap empty string with nils to be handled by some->
          ((fn [s] (if (empty? s) nil s))) 
          ;; if it is still not valid string
          ((fn [s]
             (try (Double/parseDouble s)
                  (catch NumberFormatException e
                    (log/error "Can't parse value " s)
                    nil))))))

(defn fetch [url]
  ;; TODO: fetch with retries
  ;; TODO: timeouts
  (try 
    (html/html-resource (java.net.URL. url))

    (catch java.io.FileNotFoundException e
      (log/error "File not found: " url)
      (log/error e)
      nil)

    (catch java.net.UnknownHostException e
      (log/error "Unknow host: " url)
      nil)

    (catch java.net.MalformedURLException e
      (log/error "Malformed URL: " url)
      nil)))


(defn cleanup
  "Remove whitespace charaters from string"
  [s]
  (clojure.string/trim s))

(defn falsy []
  "Returns function which accepts any number of arguments
   and always return false"
  (fn [& _] false))

(defn full-href [provider part-href]
  (let [base-url (get-in provider [:info :base-url])]
    (cond
      ;; http://sit.com/     AND      /part/of/url.html
      (and (clojure.string/ends-with? base-url "/")
           (clojure.string/starts-with? part-href "/"))
      (str base-url (subs part-href 1))

      ;; either one with /
      (or (and (clojure.string/ends-with? base-url "/")
               (not (clojure.string/starts-with? part-href "/")))
          (and (not (clojure.string/ends-with? base-url "/"))
               (clojure.string/starts-with? part-href "/")))
      (str base-url part-href)

      :else (str base-url "/" part-href))))

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
         (log/error (format "Can not resolve provider [%s]" pname)))))
