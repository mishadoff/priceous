(ns priceous.utils
  (:require [net.cgrand.enlive-html :as e]
            [clj-time.coerce :as tc]
            [clj-time.format :as tf]
            [taoensso.timbre :as log]))

;;;;;;;;;;;;;;;;;;;;;;
;; Common Utilities ;;
;;;;;;;;;;;;;;;;;;;;;;


(defn die [message] (throw (IllegalArgumentException. message)))
(defn debug [e] (log/debug e) e)

(defn to-date [unix-time]
  (->> (tc/from-long unix-time)
       (tf/unparse (tf/formatters :date-time-no-ms))))

(defn now [] (to-date (System/currentTimeMillis)))

;;;;;;;;;;;;;;;;;;;;;;;;
;; Specific Utilities ;;
;;;;;;;;;;;;;;;;;;;;;;;;

;; TESTED
(defn retrieve
  "Ensures all nodes found and return them from the specified list of nodes
  
  Examples:

    {:result nil :status :error}
    {:result 1   :status :success}
    {:result 1   :status :warn}
  "
  [nodes & {:keys [provider selector required count-strategy]
            :or   {required true     count-strategy :single}}]
  
  ;; SELECTOR AND PROVIDER ARE REQUIRED
  {:pre [provider selector]}

  ;; TODO: core match would be good
  (cond
    
    ;; No nodes found
    (empty? nodes)
    {:result nil
     :status (if required :error :success) ;; FIX we do not want to pollute namespace
     :message "[%s] No elements found using selector [%s]"}
    
    ;; Exactly one node found
    (= 1 (count nodes))
    {:result (cond
               (= count-strategy :single) (first nodes)
               (= count-strategy :multiple) nodes
               :else (die "Invalid count-strategy"))
     :status :success}
    
    ;; Found multiple nodes
    (> (count nodes) 1)
    (cond
      (= count-strategy :single)
      {:result (first nodes)
       :status :warn
       :message "[%s] Multiple elements found using selector [%s], using first"}
      
      (= count-strategy :multiple) {:result nodes :status :success}
      :else (die "Invalid count-strategy"))
    
    :else (die "Should not happen")))


;; TESTED in REPL
(defn retrieve+log
  [nodes & kseq]
  (let [{:keys [provider selector] :as ks-map} (apply hash-map kseq)
        {:keys [result status message]} (apply retrieve nodes kseq)]
    (cond
      (= :success status) result
      (= :warn status)    (do (log/warn (format message provider selector)) result)
      (= :error status)   (do (log/error (format message provider selector))  result)
      :else               (die "Invalid status"))))


;; TESTED
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


;; TESTED in REPL
(defn fetch [url]
  ;; TODO: fetch with retries
  ;; TODO: timeouts
  (try 
    (e/html-resource (java.net.URL. url))

    (catch java.io.FileNotFoundException e
      (log/error "Can't access url " url)
      nil)

    (catch java.net.MalformedURLException e
      (log/error "Malformed URL" url)
      nil)))



(defn- select-common [& keyseq]
  (fn [node selector provider]
    (apply retrieve+log (e/select node selector)
           :provider (:provider-name provider)
           :selector selector
           keyseq)))


;; TODO candidate for multimethods
(defn select-one-required [node provider selector]
  ((select-common :required true :count-strategy :single)
   node selector provider))

(defn select-one-optional [node provider selector]
  ((select-common :required false :count-strategy :single)
   node selector provider))

(defn select-mul-required [node provider selector]
  ((select-common :required true :count-strategy :multiple)
   node selector provider))

(defn select-mul-optional [node provider selector]
  ((select-common :required false :count-strategy :multiple)
   node selector provider))



(defn generic-last-page [selector]
  (fn [provider page]
    (some-> (select-one-required page provider selector)
            (e/text)
            (smart-parse-double)
            (int))))

(defn generic-page-urls [selector]
  (fn [provider page]
    (->> (select-mul-required page provider selector)
         (map #(get-in % [:attrs :href])))))


(defn property-fn [provider page]
  (fn [selector] (select-one-optional page provider selector)))

(defn cleanup [s]
  (clojure.string/trim s))

(defn text-fn [property-fn]
  (fn [selector]
    (-> (property-fn selector)
        (e/text)
        (cleanup))))

;; NOT TESTED
(defn build-spec-map [provider page selector-key selector-value]
  (let [select-cells-fn (fn [selector]
                          (->> (select-mul-optional page provider selector)
                               (map e/text)
                               (map cleanup)))
        ekeys   (select-cells-fn selector-key) 
        evalues (select-cells-fn selector-value)]
    
    ;; print warn message
    (when (not= (count ekeys) (count evalues))
      (log/warn (format "[%s] Number of keys don't match with number of values"
                        (:provider-name provider))))
    
    (zipmap ekeys evalues)))
