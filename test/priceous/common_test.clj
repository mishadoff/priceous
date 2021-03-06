(ns priceous.common-test
  (:require [clojure.test :refer :all]
            [priceous.utils :as u]
            [taoensso.timbre :as log])
  (:import (java.io File)))

;; common package for testing different providers

(defn testing-categories [cats]
  (testing "Just validate get-categories follows the contract"
    (is (pos? (count cats)))                                ;; at least one category exists
    (is (vector? cats))                                     ;; we don't break the type

    (doseq [{name :name template :template} cats]           ;; for each category
      (is (not (empty? name)))                              ;; category name is not empty
      (is (or (.startsWith template "http://")
              (.startsWith template "https://"))             ;; template is url
      (is (.contains template "%s"))                        ;; template has a placeholder
      )

    ;; no duplicate category names found
    (is (= (count cats) (count (->> cats (map :name) (into #{})))))
    ;; no duplicate urls found
    (is (= (count cats) (count (->> cats (map :template) (into #{})))))

    )))

(defn provider-doc [node->doc-fn provider test-in]
  (-> (node->doc-fn provider {:page (u/read-edn test-in) :link nil})
      (dissoc :timestamp)
      ))

(defn provider-doc-by-node [node->doc-fn provider nmap]
  (-> (node->doc-fn provider nmap) (dissoc :timestamp) ))

(defn load-cases [path]
  (let [cases (->> (file-seq (File. path))
                   (map (fn [f] (.getAbsolutePath f)))
                   (filter (fn [fname] (.endsWith fname "_in.edn")))
                   (sort)
                   (map (fn [in] [in (.replace in "_in.edn" "_out.edn") (.replace in "_in.edn" "_meta.edn")])))]
    (log/debug (format "Cases [%d] loaded: %s" (count cases) (seq (map first cases))))
    cases
    ))

(defn save [url name]
  (spit name (seq (u/fetch url))))

(defn apply-meta [provider meta-path]
  (let [meta (u/read-edn-silent meta-path)]
    (cond
      (not (nil? (:category meta)))
      (assoc-in provider [:state :category] (:category meta))
      :else provider)))