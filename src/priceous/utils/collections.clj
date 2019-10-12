(ns priceous.utils.collections)

;;;

(defn deep-merge
  "Recursively merges maps. If keys are not maps, the last value wins."
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

;;;

(defn cleanup
  "Remove whitespace charaters from string
   Empty string becames nil"
  [s]
  (some->
    s
    (.replaceAll "&nbsp;" " ")
    (.replaceAll "\\s+" " ")
    (clojure.string/trim)
    ((fn [s] (if (empty? s) nil s)))))

;;;

(defn cat-items
  "Concatenate set of items into space delimited string, nils skipped"
  [& items]
  (->> items
       (remove nil?)
       (interpose " ")
       (apply str)
       (cleanup)))
