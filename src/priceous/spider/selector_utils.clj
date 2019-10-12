(ns priceous.selector-utils
  (:require [priceous.utils :as u]
            [priceous.provider :as p]
            [taoensso.timbre :as log]
            [net.cgrand.enlive-html :as html]))


(declare
 find-nodes
 find-link
 select
 select+
 select?
 select*+
 select*?
 text-fn
 last-page
 retrieve+log
 with-selectors
 )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn find-nodes
  "Given the provider and whole page as a node, return all nodes
   by using node-selector from provider configuration
   Return all nodes as a map {:node node}"
  [provider page]
  (->> (p/node-selector provider)
       (select*+ page provider)
       (map (fn [n] {:node n}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn find-link
  "Apply link-selector to the node context to find item urls
  Despite of link-selector type it always resolves to full href
  Returns nodemap eith link key assoced"
  [provider {node :node :as node-map}]
  (assoc node-map :link
         (-> (select+ node provider (p/link-selector provider) :context node-map)
             (get-in [:attrs :href])
             (#(cond->> % (p/link-selector-relative? provider) (u/full-href provider)))
             ((fn [link]
                (let [link-fix-fn (get-in provider [:configuration :link-fixer])]
                  (if link-fix-fn (link-fix-fn link) link))))
             )))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- select
  "Return function which applies selector to node
   and log errors/warnings according rules specified in keyseq
   Also keyseq could pass a context for extra log information"
  [& keyseq]
  (fn [node provider selector & external-keyseq]
    (apply retrieve+log (html/select node selector)
           :provider provider
           :selector selector
           (concat keyseq external-keyseq))))

;; all these functions have signature
;; (fn [node provider selector & external-keyseq])

(def select+  (select :required true  :count-strategy :single))
(def select?  (select :required false :count-strategy :single))
(def select*+ (select :required true  :count-strategy :multiple))
(def select*? (select :required false :count-strategy :multiple))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO refactor in idiomatic way
(defn- retrieve+log
  "Ensures all nodes found and return them from the
   specified list of nodes

  Examples:
    {:result nil :status :error}
    {:result 1   :status :success}
    {:result 1   :status :warn}
  "
  [nodes & {:keys [provider selector required count-strategy context]
            :or   {required true
                   count-strategy :single
                   context {}}}]
  ;; SELECTOR AND PROVIDER ARE REQUIRED
  ;; TODO validate count strategy as well
  {:pre [provider selector]}

  (let [context-str (or (:link context) (:tag (:node context)))]
    (cond

      ;; No nodes found, log error if it was required adn return empty
      (empty? nodes)
      (do
        (when required
          (log/error
           (format "[%s] No elements found using selector %s in context %s"
                   (p/pname provider) selector context-str)))
        nil)

      ;; Exactly one node found
      (= 1 (count nodes))
      (cond
        (= count-strategy :single) (first nodes)
        (= count-strategy :multiple) nodes
        :else (u/die (format "Invalid count strategy [%s]" count-strategy)))

      ;; Found multiple nodes
      (> (count nodes) 1)
      (cond
        (= count-strategy :single)
        (do
          (log/warn
           (format "[%s] Multiple elements found = %d using selector [%s] in context %s, using first"
                   (p/pname provider) (count nodes) selector context-str))
          (first nodes))

        (= count-strategy :multiple) nodes
        :else (u/die (format "Invalid count strategy [%s]" count-strategy)))
      :else (u/die "Should not happen"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn text-fn [property-fn]
  (fn [selector]
    (some-> (property-fn selector)
            (html/text)
            (u/cleanup))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn img-fn [property-fn]
  (fn [selector]
    (some-> (property-fn selector)
            (get-in [:attrs :src]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn last-page [provider page]
  "Return the last page for provider, given page enlive structure"
  (let [last-page-num
        (some->> (select*? page provider (p/last-page-selector provider))
                 ((fn [nodes]
                    (let [select-post-fn (get-in provider [:configuration :last-page-process-fn])]
                      (if select-post-fn (map select-post-fn nodes) nodes))))
                 (map html/text)
                 (map u/cleanup)
                 (remove nil?)
                 (map #(re-matches #"\d+" %))
                 (map u/smart-parse-double)
                 (remove nil?)
                 (sort)
                 (last)
                 (int))]
    (or last-page-num 1)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro with-selectors
  "Create a lexical context bound to provider and nodemap objects
   If strategy is :heavy, all query functions are bound to whole page
   If strategy is :light, all query functions are bound to the context node

   Macro exposes in its body scope following functions:
    q+     query by selector, result should be single and required
    q?     query by selector, result should be single and optional
    q*     query by selector, result should be multiple items, required
    text+  query text by selector, same as q+
  "

  [provider nodemap & body]
  `(let [node# (if (p/heavy? ~provider) (:page ~nodemap) (:node ~nodemap))
         ~'q+ (fn [selector#] (select+ node# ~provider selector# :context ~nodemap))
         ~'q? (fn [selector#] (select? node# ~provider selector# :context ~nodemap))
         ~'q* (fn [selector#] (select*+ node# ~provider selector# :context ~nodemap))
         ~'q*? (fn [selector#] (select*? node# ~provider selector# :context ~nodemap))
         ~'text+ (text-fn ~'q+)
         ~'text? (text-fn ~'q?)
         ~'img (img-fn ~'q+)]
     (if node# ~@body nil)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO tests
(defn spec-with-only-key
  "Create map of specs by providing only key selector,
   value for this key will be full node content with key selector removed
  "
  [nodes key-selector]
  (->> nodes
       (map (fn [n]
              (->> [(html/select n key-selector) (html/at n key-selector nil)]
                   (mapv first)
                   (mapv html/text)
                   (mapv u/cleanup))))
       (into {})))

(defn spec-with-split
  "Create map of specs from the nodes by splitting on provided separator"
  [split nodes]
  (->> nodes
       (map html/text)
       (map (fn [s]
              (some->> (.split s ":" 2)
                       (seq)
                       (map u/cleanup)
                       (into []))))
       (filter (fn [v] (= (count v) 2)))
       (into {})))