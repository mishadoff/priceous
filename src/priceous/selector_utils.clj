(ns priceous.selector-utils
  (:require [priceous.utils :as u]
            [priceous.provider :as p]
            [taoensso.timbre :as log]
            [net.cgrand.enlive-html :as html]))


(declare
 retrieve                       ;; TESTED
 retrieve+log                   ;; TODO: TEST
 generic-next-page?             ;; TESTED
 generic-page-urls              ;; TESTED
 generic-page-urls-with-prefix  ;; TESTED
 property-fn                    ;; TODO: TEST
 text-fn                        ;; TODO: TEST
 build-spec-map                 ;; TESTED
 select-common                  ;; PRIVATE
 select-one-req                 ;; PRIVATE
 select-mul-req                 ;; PRIVATE
 select-one-opt                 ;; PRIVATE
 select-mul-opt                 ;; PRIVATE
 )


(defn- select-common
  "Return function which applies selector to node
   and log errors/warnings according rules specified in keyseq

   Side Effects.
  "
  [& keyseq]
  (fn [node selector provider]
    (apply retrieve+log
           (html/select node selector)
           :provider provider
           :selector selector
           keyseq)))


(defn retrieve
  "Ensures all nodes found and return them from the
   specified list of nodes
  
  Examples:
    {:result nil :status :error}
    {:result 1   :status :success}
    {:result 1   :status :warn}
  "
  [nodes & {:keys [provider selector required count-strategy]
            :or   {required true     count-strategy :single}}]
  
  ;; SELECTOR AND PROVIDER ARE REQUIRED
  {:pre [provider selector]}

  (cond
    
    ;; No nodes found
    (empty? nodes)
    {:result nil
     :status (if required :error :success)
     :message "[%s] No elements found using selector [%s]"}
    
    ;; Exactly one node found
    (= 1 (count nodes))
    {:result (cond
               (= count-strategy :single) (first nodes)
               (= count-strategy :multiple) nodes
               :else (u/die "Invalid count-strategy"))
     :status :success}
    
    ;; Found multiple nodes
    (> (count nodes) 1)
    (cond
      (= count-strategy :single)
      {:result (first nodes)
       :status :warn
       :message "[%s] Multiple elements found using selector [%s], using first"}
      
      (= count-strategy :multiple) {:result nodes :status :success}
      :else (u/die "Invalid count-strategy"))
    :else (u/die "Should not happen")))

(defn retrieve+log
  "Side-effect retrieve companion. It unwraps the real value
  from retrieve result and log any errors and warnings"
  [nodes & kseq]
  (let [{:keys [provider selector] :as ks-map} (apply hash-map kseq)
        {:keys [result status message]} (apply retrieve nodes kseq)]
    (cond
      (= :success status) result
      (= :warn status)    (do (log/warn (format message (p/get-provider-name provider) selector)) result)
      (= :error status)   (do (log/error (format message (p/get-provider-name provider) selector))  result)
      :else               (u/die "Invalid status"))))

(defn generic-next-page?
  "Return a predicate function (fn [provider page])
   which yield
    - true, if there are more pages for current state for provider
    - false, otherwise"
  [selector]
  (fn [provider page]
    (some-> (select-one-req page provider selector)
            (html/text)
            (u/smart-parse-double)
            (int)
            ((fn [p]
               (< (get-in provider [:state :page-current]) p))))))

(defn generic-page-urls
  "Return a function (fn [provider page])
  which return all links from the page.
  Selector should return path to <a> tag"
  [selector]
  (fn [provider page]
    (->> (select-mul-req page provider selector)
         (map #(get-in % [:attrs :href])))))

(defn generic-page-urls-with-prefix
  "Return a function (fn [provider page])
  which return all links from the page prefixed with [prefix] arg.
  Selector should return path to <a> tag.
  
  This function mostly needed if page contains relative links
  so we need to pass base path as a prefix to get absolute url"
  [selector prefix]
  (fn [provider page]
    (->> (select-mul-req page provider selector)
         (map #(get-in % [:attrs :href]))
         (map #(str prefix %)))))

(defn property-fn [provider page]
  (fn [selector] (select-one-opt page provider selector)))

(defn text-fn [property-fn]
  (fn [selector]
    (-> (property-fn selector)
        (html/text)
        (u/cleanup))))

(defn build-spec-map
  "Accepts selector for keys and selector for values
  and build a map from page representation

  If key-selector and value-selector return different amount
  of items, min-match map is build
  "
  [provider page selector-key selector-value]
  (let [select-cells-fn (fn [selector]
                          (->> (select-mul-opt page provider selector)
                               (map html/text)
                               (map u/cleanup)))
        ekeys   (select-cells-fn selector-key) 
        evalues (select-cells-fn selector-value)]
    (when (not= (count ekeys) (count evalues))
      (log/warn (format "[%s] Number of keys don't match with number of values" (get-in provider [:info :name]))))
    (zipmap ekeys evalues)))


(defn select-one-req [node provider selector]
  ((select-common :required true :count-strategy :single)
   node selector provider))

(defn select-one-opt [node provider selector]
  ((select-common :required false :count-strategy :single)
   node selector provider))

(defn select-mul-req [node provider selector]
  ((select-common :required true :count-strategy :multiple)
   node selector provider))

(defn select-mul-opt [node provider selector]
  ((select-common :required false :count-strategy :multiple)
   node selector provider))
