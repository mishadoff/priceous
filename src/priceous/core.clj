(ns priceous.core
  (:require [priceous.solr :as solr]
            [priceous.utils :as u]
            [priceous.config :as config]
            [priceous.flow :as flow]

            [clojure.data.csv :as csv]
            [clojure.java.io :as io]            
            [taoensso.timbre :as log]
            
            ;; providers
            [priceous.provider.goodwine :as gw]
            [priceous.provider.winetime :as winetime]
            [priceous.provider.rozetka :as rozetka]
            [priceous.provider.fozzy :as fozzy]
            [priceous.provider.metro :as metro]
            [priceous.provider.novus :as novus]
            [priceous.provider.auchan :as auchan]
            [priceous.provider.megamarket :as megamarket]
            [priceous.provider.elitochka :as elitochka]
            )
  (:gen-class))

(defn- monitor-provider [state provider]
  ;; TODO fix npe
  (log/info state)
  (try
    (let [items
          (cond
            (:category provider)
            (let [cat-fn (get-in provider [:functions :categories])
                  cats (cat-fn provider)]
              (->> (map (fn [[cat-name cat-url]]
                          (-> provider
                              (assoc-in [:state :page-template] cat-url)
                              (assoc-in [:state :category] cat-name))) cats)
                   (map flow/process)
                   (apply concat)))
            
            :else (flow/process provider))

          provider-name (get-in provider [:info :name])]
      (cond
        ;; nothing found
        (empty? items)
        (do 
          (log/warn (format "[%s] No items found" provider-name))
          state) 

        :else
        (do
          (log/info (format "[%s] Found %s items" provider-name (count items)))

          ;; solr appender
          (solr/write provider items)        

          ;; return state as it will be caried to the next
          (update-in state [:total] + (count items)))))
    (catch Exception e
      (log/error e)
      ;; something bad happened do not update state atom
      state)))

(defn monitor-all [providers]
  (try
    (let [start (System/currentTimeMillis)]
      (log/info "Start monitoring prices")
      
      ;; process each provider
      (let [final-state
            (reduce
             ;; sequentially monitor each provider
             (fn [acc provider]
               (monitor-provider acc provider))
             ;; initial state
             {:total 0}
             providers)]
        
        (log/info (format "Succecfully processed %s items in %s seconds"
                          (or (:total final-state) 0)
                          (/ (- (System/currentTimeMillis) start) 1000.0)))))
    
    (catch Exception e
      (log/error "Processing failed")
      (log/error e))
    
    ))

(def provider-map
  {
   "metro"          metro/provider
   "auchan"         auchan/provider
   "novus"          novus/provider
   "fozzy"          fozzy/provider
   "goodwine"       gw/provider  
   "rozetka"        rozetka/provider
   "megamarket"     megamarket/provider
   "elitochka"      elitochka/provider
   "winetime"       winetime/provider
   ;; "polyana"
   ;; "vintagemarket"
   ;; "silpo?"
   ;; "elit-alco"
   ;; "alcovegas"
   
   })

(defn gather [provider-names]
  (log/info provider-names)
  (monitor-all (map #(get provider-map %) provider-names)))
