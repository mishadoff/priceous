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

(defn smart-parse-double [s]
  (some-> s
          ;; repalce commas to periods
          (clojure.string/replace "," ".")
          ;; remove all alienm symbols
          (clojure.string/replace #"[^0-9\\.]+" "")
          ;; swap empty string with nils to be handled by some->
          ((fn [s] (if (empty? s) nil s))) 
          ;; if it is still not vali string
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
