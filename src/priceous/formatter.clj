(ns priceous.formatter)

(defn processing-page [provider last-page-num]
  (format "[%s | %s] Processing page %s/%s"
          (get-in provider [:info :name])
          (get-in provider [:state :category])
          (get-in provider [:state :page-current])
          last-page-num))

(defn category-processed [provider size]
  (format "[%s | %s] Category processed. Found %s items"
          (get-in provider [:info :name])
          (get-in provider [:state :category])
          size))

(defn succesfully-processed-all [n-items elapsed]
  (format "Succesfully processed %s items in %s seconds" n-items elapsed))
