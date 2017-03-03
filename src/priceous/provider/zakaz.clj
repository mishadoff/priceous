(ns priceous.provider.zakaz
  (:require [clojure.string :as str]
            [net.cgrand.enlive-html :as html]
            [priceous.provider :as p]
            [priceous.selector-utils :as su]
            [priceous.utils :as u]
            [taoensso.timbre :as log]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn node->document [provider nodemap]
  (su/with-selectors provider nodemap
    (-> {}
        (assoc :provider (p/pname provider))
        (assoc :name (text+ [:.one-product-name]))
        (assoc :link (-> (q+ [:.one-product-link])
                         (get-in [:attrs :href])
                         (#(u/full-href provider %))))
        (assoc :image (-> (q? [:.one-product-image :img])
                          (get-in [:attrs :src])))
        (assoc :type nil)
        (assoc :timestamp (u/now))
        (assoc :available true)
        (assoc :price (some-> (text+ [:.one-product-price])
                              (u/smart-parse-double)
                              (/ 100.0)))
        ;; is sale
        ((fn [doc]
           (let [sale-price (some-> (text? [:.badge.right-up-sale-bage])
                                    (u/smart-parse-double)
                                    (/ 100.0))]
             (cond
               (and (:price doc) sale-price)
               (-> doc
                   (assoc :sale true)
                   (assoc :sale-description (format "старая цена %.2f" sale-price)))
               :else (assoc doc :sale false)))))
        )))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def last-page-selector [:.catalog-pagination :a])
(def node-selector [:.one-product])
