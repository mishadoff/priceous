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
 select-one-req                 ;; TODO: TEST
 select-mul-req                 ;; TODO: TEST
 select-one-opt                 ;; TODO: TEST
 select-mul-opt                 ;; TODO: TEST
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
      (= :warn status)    (do (log/warn (format message (p/pname provider) selector)) result)
      (= :error status)   (do (log/error (format message (p/pname provider) selector))  result)
      :else               (u/die "Invalid status"))))

(defn generic-next-page?
  "Return a predicate function (fn [provider page])
   which yield
    - true, if there are more pages for current state for provider
    - false, otherwise"
  [selector]
  (fn [provider page]
    (boolean
     (some-> (select-one-opt page provider selector)
             (html/text)
             (u/smart-parse-double)
             (int)
             ((fn [p]
                (< (get-in provider [:state :page-current]) p)))
             ))))

;; node-selector, url-selector
(defn generic-page-urls
  "Return a function (fn [provider page])
  which return all links from the page and
  context in which they were found

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
  [provider page selector-key selector-value
   & {:keys [keys-post-fn vals-post-fn]
      :or {keys-post-fn identity vals-post-fn identity}}]
  (let [select-cells-fn (fn [selector post-fn]
                          (->> (select-mul-opt page provider selector)
                               (map post-fn)
                               (map html/text)
                               (map u/cleanup)))
        ekeys   (select-cells-fn selector-key keys-post-fn) 
        evalues (select-cells-fn selector-value vals-post-fn)]
    (when (not= (count ekeys) (count evalues))
      (log/warn (format "[%s] Number of keys don't match with number of values" (get-in provider [:info :name]))))
    #_(log/debug (seq ekeys))
    #_(log/debug (seq evalues))
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

(defn create-last-page-fn [selector]
  "Create closure on selector and return fn [provider page] -> int
   which return last page of the current provider or category
   If failed, returns 1 as a last page"
  (fn [provider page]
    (let [last-page-num 
          (some->> (select-mul-req page provider selector)
                   (map html/text)
                   (remove #{"»" "..."})
                   (map u/smart-parse-double)
                   (sort)
                   (last)
                   (int))]
      (or last-page-num 1))))


(defn find-nodes
  "Given the provider and whole page as a node, return all nodes
   by using node-selector from provider configuration
   Return all nodes as a map {:node node}"
  [provider page]
  (->> (get-in provider [:configuration :node-selector])
       (select-mul-req page provider)
       (map (fn [n] {:node n}))))

(defn find-link
  "Apply link-selector to the node context to find item urls
  Despite of link-selector type it always resolves to full href
  Returns nodemap eith link key assoced"
  [provider {node :node :as node-map}]
  (assoc node :link
         (-> (select-one-req node provider (p/link-selector provider))
             (get-in [:attrs :href])
             (#(cond->> % (p/link-selector-relative? provider) (u/full-href provider))))))

(defn last-page [provider page]
  ((create-last-page-fn (p/last-page-selector provider))
   provider page))

