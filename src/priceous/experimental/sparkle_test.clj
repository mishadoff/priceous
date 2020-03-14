(ns priceous.experimental.sparkle-test
  (:require [sparkledriver.browser :as spark]
            [sparkledriver.element :as q]
            [clojure.tools.logging :as log]))

(defn safe-inc [n]
  (if n (inc n) 1))

;; Scrap context

(defn lightweight-spider [browser spider-context]
  (let [{:keys [page]} spider-context]
    (log/infof "Requesting page: %d" page)
    (let [url (format "https://goodwine.com.ua/viski.html?dir=asc&p=%d" page)
          products (-> (spark/fetch! browser url)
                       (q/find-by-css* ".ajax-pager-products-container > li.default"))]
      (log/infof ">> Retrieved %d products" (count products))
      (for [p products]
        (let [p-name (-> p
                         (q/find-by-css ".textBlock > a > span")
                         (q/text))
              link (-> p
                       (q/find-by-css ".textBlock > a")
                       (q/attr "href"))]
          (log/info p-name "->" link)
          {:product-name p-name
           :link link}
          )))))

(defn stop? [scrap-context]
  (let [{:keys [previous-products current-products]} scrap-context]
    ;; stop when next poll return the same products
    (and (= previous-products current-products)
         (not= current-products nil)
         (not= previous-products nil))))

(defn advance [scrap-context]
  (update scrap-context :page inc))

(defn product-id [product] (:link product))

(defn add-products [db products]
  (doseq [p products]
    (swap! db assoc (product-id p) p)))

(defn scrap []
  (let [db (atom {})]
    (spark/with-browser [browser (spark/make-browser)]
      (log/info "Browser is ready.")
      (loop [scrap-context {:page 1}]
        (cond
          (stop? scrap-context)
          (do (log/infof "Scrapping finished, got %d items" (count @db)))
          :else
          (do
            (let [products (lightweight-spider browser scrap-context)
                  _ (add-products db products)
                  previous-products (:current-products scrap-context)
                  newsc (-> scrap-context
                            (advance)
                            (assoc :previous-products previous-products)
                            (assoc :current-products products))]
              (log/infof "Unique products: %d" (count @db))
              (recur newsc))))))))
