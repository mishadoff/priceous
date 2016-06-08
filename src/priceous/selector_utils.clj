(ns priceous.selector-utils
  (:require [priceous.utils :as u]
            [priceous.provider :as p]
            [taoensso.timbre :as log]
            [net.cgrand.enlive-html :as html]))


(declare
 select-common                  ;; TODO: TEST
 retrieve                       ;; TESTED
 retrieve+log                   ;; TODO: TEST
 select-one-req                 ;; TODO: TEST
 select-mul-req                 ;; TODO: TEST
 select-one-opt                 ;; TODO: TEST
 select-mul-opt                 ;; TODO: TEST
 generic-next-page?             ;; TODO: TEST
 generic-page-urls              ;; TODO: TEST
 generic-page-urls-with-prefix  ;; TODO: TEST
 property-fn                    ;; TODO: TEST
 text-fn                        ;; TODO: TEST
 build-spec-map                 ;; TODO: TEST
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
  [nodes & kseq]
  (let [{:keys [provider selector] :as ks-map} (apply hash-map kseq)
        {:keys [result status message]} (apply retrieve nodes kseq)]
    (cond
      (= :success status) result
      (= :warn status)    (do (log/warn (format message (p/get-provider-name provider) selector)) result)
      (= :error status)   (do (log/error (format message (p/get-provider-name provider) selector))  result)
      :else               (u/die "Invalid status"))))


;; TODO candidate for multimethods
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

(defn generic-next-page? [selector]
  (fn [provider page]
    (some-> (select-one-req page provider selector)
            (html/text)
            (u/smart-parse-double)
            (int)
            ((fn [p]
               (< (get-in provider [:state :page-current]) p))))))

(defn generic-page-urls [selector]
  (fn [provider page]
    (->> (select-mul-req page provider selector)
         (map #(get-in % [:attrs :href])))))

(defn generic-page-urls-with-prefix [selector prefix]
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

(defn build-spec-map [provider page selector-key selector-value]
  (let [select-cells-fn (fn [selector]
                          (->> (select-mul-opt page provider selector)
                               (map html/text)
                               (map u/cleanup)))
        ekeys   (select-cells-fn selector-key) 
        evalues (select-cells-fn selector-value)]
    
    ;; print warn message
    (when (not= (count ekeys) (count evalues))
      (log/warn (format "[%s] Number of keys don't match with number of values"
                        (get-in provider [:info :name]))))
    
    (zipmap ekeys evalues)))
