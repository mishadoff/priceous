(ns priceous.provider.zakaz-react
  (:require [clojure.string :as str]
            [net.cgrand.enlive-html :as html]
            [priceous.provider :as p]
            [priceous.selector-utils :as su]
            [priceous.utils :as u]
            [clojure.tools.logging :as log]
            [clj-http.client :as http]
            [cheshire.core :as json]))

(declare
 query
 query-struct
 node->document)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- query-struct
  "Build a structure for next query to zakaz react based api"
  [provider]
  {:meta {}
   :request [{:args {:store_id (get-in provider [:custom :store_num])
                     :slugs ["eighteen-plus"]
                     :facets []
                     :sort "catalog"
                     :extended true}
              :v "0.1"
              :type "store.products"
              :id "catalog"
              :offset (get-in provider [:state :page-current])
              :join [{:apply_as "facets_base" :on ["slug" "slug"]
                      :request {:v "0.1"
                                :type "store.facets"
                                :args {:store_id "$request.[-2].args.store_id"
                                       :slug "$request.[-2].args.slugs|first"
                                       :basic_facets []}}}
                     {:apply_as "category_tree" :on ["slug" "requested_slug"]
                      :request {:v "0.1"
                                :type "store.department_tree"
                                :args {:store_id "$request.[-2].args.store_id"
                                       :slug "$request.[-2].args.slugs|first"}}}]}]})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- node->document
  "For API based provides node is an item obtained by API call"
  [provider item]
  (-> {}
      (assoc :excise true)
      (assoc :trusted true)
      (assoc :provider (get-in provider [:info :name]))
      (assoc :timestamp (u/now))
      (assoc :link (-> (str (get-in item [:ean]) "/" (get-in item [:slug]))
                       (#(u/full-href provider %))))
      (assoc :image (get-in item [:main_image :s150x150]))
      (assoc :name (get-in item [:name]))
      (assoc :alcohol (-> (get-in item [:extended_info :alcohol])
                          (u/smart-parse-double)))
      #_(assoc :type (get-in item [:extended_info :type])) ;; some wrong type
      (assoc :producer (get-in item [:extended_info :tm]))
      (assoc :volume (let [v (get-in item [:volume])]
                       (if v (/ v 1000) v)))
      (assoc :country (u/cleanup (str (get-in item [:extended_info :country]) " "
                                      (get-in item [:extended_info :region]))))
      (assoc :product-code (str (p/pname provider) "_" (get-in item [:sku])))
      (assoc :sale (get-in item [:sale]))
      (assoc :available (get-in item [:available]))
      (assoc :price (/ (get-in item [:price]) 100.0))

      ((fn [doc]
         (let [p (get-in item [:old_price])]
           (if p
             (assoc doc :sale-description (format "старая цена %.2f" (/ p 100.0)))
             doc))))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn query [provider]
  (try
    (let [result (http/post
                  (str (get-in provider [:info :base-url]) "/api/query.json")
                  {:body (json/generate-string (query-struct provider))
                         :content-type :json
                   :accept :json})]
      (if (not= 200 (:status result))
        (do (log/error (format "Problem sending request, status %s" (:status result)))
            {:status :error :response {}})
        (-> {:status :success}
            (assoc :rawdata (json/parse-string (:body result) true))
            ((fn [s]
               (-> s
                   (assoc :docs (->> (:rawdata s)
                                     :responses
                                     first
                                     :data
                                     :items
                                     first
                                     :items
                                     (map (fn [i] (node->document provider i)))))
                   (assoc :num-pages (->> (:rawdata s)
                                          :responses
                                          first
                                          :data
                                          :items
                                          first
                                          :num_pages))))))))



    (catch Exception e
      (log/error e)
      {:status :error :response {}})))
