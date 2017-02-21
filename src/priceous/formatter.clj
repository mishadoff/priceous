(ns priceous.formatter)

(defn processing-page [provider]
  (format "[%s | %s] Processing page %s"
          (get-in provider [:info :name])
          (get-in provider [:state :category])
          (get-in provider [:state :page-current])))

(defn category-processed [provider size]
  (format "[%s | %s] Category processed. Found %s items"
          (get-in provider [:info :name])
          (get-in provider [:state :category])
          size))

(defn succesfully-processed-all [n-items elapsed]
  (format "Succesfully processed %s items in %s seconds"
          n-items elapsed))
