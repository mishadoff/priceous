(ns priceous.spider.solr
  (:require [clj-time.coerce :as tc]
            [clojure.string :as str]
            [flux.core :as flux]
            [flux.http :as http]
            [flux.query :as query]
            [priceous.config :as config]
            [priceous.provider :as p]
            [priceous.utils :as u]
            [taoensso.timbre :as log])
  (:import [org.apache.solr.client.solrj.util ClientUtils]))

(declare
 resolve-page
 resolve-sort
 resolve-available
 process-query
 range-processor)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn query [params ctx]
  (let [q (:query params)
        page (resolve-page params)
        sorting (resolve-sort params)
        available (resolve-available params)
        perpage (config/prop [:view :per-page] 10)]
    (try
      (flux/with-connection
        (http/create
         (get-in @config/properties [:solr :host])
         (keyword (get-in @config/properties [:solr :collection])))
        (let [random? (= "random" (:sort params))
              {:keys [q fq]} (process-query params)
              request {:q     q
                       :q.op  "AND"
                       :df    "text"
                       :start (if random? 0 (* perpage (dec page)))
                       :rows  (if random? 1 perpage)
                       :sort  sorting
                       :fq    (str/join " AND " fq)}
              response (flux/request (query/create-query-request request))
              total (if random? 1 (get-in response [:response :numFound]))]
          (log/info (format "[%s] Completed SolrQuery [%s] found %s items" (:ip ctx) q total))
          {:status :success :data response :total total}))
      (catch Exception e
        (log/error e)
        {:status :error :response {}}))))

(defn- process-provider-pivots [response]
  (let [pivot (first (vals (get-in response [:facet_counts :facet_pivot])))]
    (mapv (fn [pp] {:name (:value pp)
                    :total (:count pp)
                    :available (->> (:pivot pp)
                                    (filter #(= (:value %) true))
                                    (first)
                                    (:count))}) pivot)))

(defn stats [ctx]
  (try
    (flux/with-connection
      (http/create (config/prop [:solr :host])
                   (keyword (config/prop [:solr :collection])))
      (log/info (format "[%s] Requested StatsRequest" (:ip ctx)))
      (let [response
            (flux/request
             (query/create-query-request
              {:q "*"
               :q.op "AND"
               :start 0
               :rows 0
               :df "text"
               :json.facet.providers
               "{type:terms,
                 field:provider,
                 limit:100,
                 facet:{
                   ts:\"max(timestamp)\",
                   available:{
                     type:terms,
                     field:available}}}"}))]
        {:status :success
         :response {:total (get-in response [:response :numFound])
                    :providers (->> (get-in response [:facets :providers :buckets])
                                    (mapv (fn [bkt]
                                            {:name (:val bkt)
                                             :total (:count bkt)
                                             :ts (-> (:ts bkt) (.getTime) u/to-date)
                                             :available (or (some->> (get-in bkt [:available :buckets])
                                                                     (filter :val)
                                                                     (first)
                                                                     (:count))
                                                            0)})))}}))
    (catch Exception e
      (log/error e)
      {:status :error :response {}})))


(defn transform-dashes-to-underscores [m]
  (into {} (map (fn [[k v]]
                  [(keyword (subs (clojure.string/replace (str k) "-" "_") 1))
                   v]) m)))


(defn write [provider items]
  (try
    (flux/with-connection
      (http/create
       (get-in @config/properties [:solr :host])
       (keyword (get-in @config/properties [:solr :collection])))

      (log/info "Connections to SOLR established")

      ;; log deltas
      (let [num-before (-> (flux/request (query/create-query-request
                                          {:q "*" :rows 0
                                           :df "text"
                                           :q.op "AND"
                                           :fq (format "provider:%s" (p/pname provider))}))
                           (get-in [:response :numFound]))
            delta (- (count items) num-before)]

        (log/info (format "[%s] Items delta %d" (p/pname provider) delta))
        (when (< delta -50) ;; TODO configurable alert for deltas
          (log/warn "High delta change detected, probably something wrong")))

      ;; delete all documents for this provider because currently we interested
      ;; in recent items
      (flux/delete-by-query (str "provider:" (get-in provider [:info :name])))

      ;; transform and add to solr
      (->> items
           (map transform-dashes-to-underscores)
           (flux/add))

      (flux/commit)
      (log/info (format "Pushed to solr %s items" (count items))))
    (catch Exception e
      (log/error "Pushing to solr failed")
      (log/error e))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- resolve-page
  "Parse the page from params into number
  If cannot parse, return 1"
  [params]
  (let [page (:page params)]
    (or (some-> page
                (try (Integer/parseInt page)
                     (catch NumberFormatException e nil)))
        1)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- resolve-sort
  "Parse the sort info from params, and build solr query sort params
  If cannot parse, use default price+asc"
  [params]
  (let [{:keys [sort]} params]
    (cond (= sort "random") (format "random_%d desc" (rand-int Integer/MAX_VALUE))
          :else
          (get {"cheap"     "price asc"
                "expensive" "price desc"
                "relevant"  "score desc"}
               (:sort params)
               "price asc"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- resolve-available
  "Parse the available info from params, and build solr query available param
  If cannot parse, use default available:true"
  [params]
  (get #{"true" "false" "all"} (:available params) "true"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO find how instaparse can help us for this
(defn process-query
  "Process query for specific values, ranges, etc.
   Return structure for further processing."
  [params]
  (let [q (:query params)
        a (resolve-available params)]
    (cond
      ;; special hidden syntax allow to pass raw query
      ;; should start with !
      ;; filters not applied
      (clojure.string/starts-with? q "!") {:q (subs q 1) :fq []}

      :else
      (-> {:q (.toLowerCase q) :fq []}

          ;; add avaialable filter
          ((fn [req] (if (= a "all")
                       req
                       (update req :fq conj (format "available:%s" a)))))

          ;; TODO DSL for this?
          ;; add sale filter
          ((fn [req]
             (let [sale-regex #"\b(акция|акции)\b"
                   match (re-seq sale-regex (:q req))]
               (cond (not match) req
                     :else
                     {:q  (str/replace (:q req) sale-regex "")
                      :df "text"
                      :q.op "AND"
                      :fq (conj (:fq req) "sale:true")}))))

          ;; add news filter
          ((fn [req]
             (let [new-regex #"\b(новинки)\b"
                   match (re-seq new-regex (:q req))]
               (cond (not match) req
                     :else
                     {:q  (str/replace (:q req) new-regex "")
                      :df "text"
                      :q.op "AND"
                      :fq (conj (:fq req) "item_new:true")}))))

          (range-processor "крепость" "alcohol")
          (range-processor "обьем" "volume")
          (range-processor "сахар" "wine_sugar")
          (range-processor "цена" "price")

          ;; cleanup and escape query
          (update :q (fn [query]
                       (let [qc (u/cleanup query)]
                         (if (empty? qc)
                           "*" ;; request all
                           (ClientUtils/escapeQueryChars qc)))))))))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn range-processor [req keyw field]
  (let [range-regex-base "(?:\\s+от\\s+(\\d+(?:\\.\\d+)?))?(?:\\s+до\\s+(\\d+(?:\\.\\d+)?))?\\b"
        range-regex (re-pattern (str "\\b" keyw range-regex-base))
        match (re-seq range-regex (:q req))]
    (cond (not match) req
          :else
          (let [[m from to] (first match)] ;; TODO we do not process multiple matches for the same filter type (reduce goes here)
            {:q  (str/replace (:q req) range-regex "")
             :df "text"
             :q.op "AND"
             :fq (conj (:fq req) (format "%s:[%s TO %s]"
                                         field
                                         (or from "*")
                                         (or to "*")))}))))

;;;;;;;;;;
