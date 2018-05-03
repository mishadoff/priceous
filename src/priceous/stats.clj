(ns priceous.stats)

(def weights
  {:provider         1    ;; basic score, present in all documents
   :name             10   ;; name is very important, required
   :link             5    ;; link important for reference
   :image            4    ;; nice to have
   :country          3    ;; nice to have
   :wine_sugar       2    ;; category dependent
   :wine_grape       2    ;; category dependent
   :vintage          2    ;; nice to have, but dependent
   :producer         3    ;; nice to have
   :type             5    ;; type is important for exploratory search
   :alcohol          5    ;; important for filters
   :description      4    ;; nice to have, for extend search
   :product-code     5    ;; important for some future features
   :available        3    ;; needed for advanced search
   :item_new         3    ;; advanced search
   :volume           6    ;; volume very important
   :price            8    ;; important, but could be missing if product is NA
   :sale             2    ;; could be missing
   :sale-description 2    ;; needed, but could be missing
   :excise           4    ;; nice to have, few practical use cases
   :trusted          4    ;; nice to have, few practical use cases
   :timestamp        5})    ;; needed for reference


(def weights-total (reduce + (vals weights)))

(defn data-coverage [doc]
  (if doc
    (some->> (keys weights)
         (map (fn [k] (if (nil? (doc k)) 0 (weights k))))
         (reduce +)
         (#(/ % weights-total))
         (double))
    0.0))

(defn data-coverage-avg [items]
  (if (empty? items)
    0.0
    (/ (->> items (map data-coverage) (reduce +))
       (count items))))