(ns priceous.utils
  (:require [net.cgrand.enlive-html :as e]
            [clj-time.coerce :as tc]
            [clj-time.format :as tf]
            [taoensso.timbre :as log]))

;;;;;;;;;;;;;;;;;;;;;;
;; Common Utilities ;;
;;;;;;;;;;;;;;;;;;;;;;

(def ensure-one-template-0 "[%s] No elements found using selector [%s]")
(def ensure-one-template-N "[%s] Multiple elements found using selector [%s], using first")

(defn ensure-one
  "Ensures and returns exactly one provider
  from the specified list of nodes

  If nodes are empty, log error and returns nil
  If nodes contain multiple elements, log warn and return first
  "
  [nodes & {:keys [required selector provider]
            :or {required true}}]
  (let [logfn (if required #(log/error %) #(log/warn %))]
    (cond
      ;; No nodes found
      (empty? nodes)

      (do (logfn (format ensure-one-template-0 provider selector))
          nil)

      ;; Ambiguity, found multiple nodes
      (> (count nodes) 1)
      (do (log/warn (format ensure-one-template-N provider selector))
          nil)

      ;; Exactly one element we good
      :else
      (first nodes)

      )))

(defn debug [e]
  (log/debug e) e)

(defn safe-parse-double [d]
  (let [num (if (empty? d) nil (Double/parseDouble d))]
    (if (or (nil? num) (zero? num)) nil num)))


(defn safe-parse-double-with-intruders [s]
  (-> s
      (clojure.string/replace "," ".")
      (clojure.string/replace #"[^0-9\\.]+" "")
      (safe-parse-double)))

(defn to-date [unix-time]
  (tf/unparse (tf/formatters :date-time-no-ms) (tc/from-long unix-time)))

(defn now []
  (to-date (System/currentTimeMillis)))

(defn fetch [url]
  (try 
    (e/html-resource (java.net.URL. url))
    (catch java.io.FileNotFoundException e
      (log/error "Can't access url " url)
      nil)))


(defn select [node selector provider]
  (some-> node
          (e/select selector)
          (ensure-one :required false :selector selector :provider name)))

