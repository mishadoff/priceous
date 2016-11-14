(ns priceous.provider.zakaz-react
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [priceous.utils :as u]
            [taoensso.timbre :as log]
            ))

(defn query-struct [provider]
  {:meta {}
   :request [{:args {:store_id (get-in provider [:custom :store_num])
                     :slugs ["hard-drinks"]
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

(defn transform-item [provider item]
  {

   :provider                (get-in provider [:info :name])
   :base-url                (get-in provider [:info :base-url])
   :icon-url                (get-in provider [:info :icon])
   :icon-url-width          (get-in provider [:info :icon-width])
   :icon-url-height         (get-in provider [:info :icon-height])
   :timestamp               (u/now)
   :link             (str (get-in provider [:info :base-url]) "/"
                          (get-in item [:ean]) "/"
                          (get-in item [:slug]))
   
   :image            (get-in item [:main_image :s150x150])
   :name             (get-in item [:name])
   :alcohol          (-> (get-in item [:extended_info :alcohol])
                         (u/smart-parse-double))
   :type             (get-in item [:extended_info :type])
   :producer          (get-in item [:extended_info :tm])
   :volume           (let [v (get-in item [:volume])]
                       (if v (/ v 1000) v))
   :country          (str (get-in item [:extended_info :country]) " "
                          (get-in item [:extended_info :region]))

   :product-code     (get-in item [:sku])
   :sale             (get-in item [:sale])
   :available        (get-in item [:available])
   :price            (/ (get-in item [:price]) 100.0)
   }
  )

(defn query [provider]
  (try 
    (let [result (http/post
                  (str (get-in provider [:info :base-url])
                       "/api/query.json")
                  
                  {:body (json/generate-string (query-struct provider))
                             :content-type :json
                             :accept :json})]
      (if (not= 200 (:status result))
        (do (log/error (format "Problem sending request, status %s" (:status result)))
            {:status :error :response {}})
        {:status :success
         :docs
         (->> (json/parse-string (:body result) true)
              :responses
              first
              :data
              :items
              first
              :items
              (map (fn [i] (transform-item provider i))))}))
    (catch Exception e
      (log/error e)
      {:status :error :reponse {}})
    )
  )

