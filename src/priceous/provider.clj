(ns priceous.provider
  (:require [taoensso.timbre :as log]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Accessors and Mutators for provider structure
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn category-function
  "Returns a function for obtaining categories from provider
   If provider do not support categories, returns a function, which
   provides default category with page template."
  [provider]
  (get-in provider
          [:configuration :categories-fn]
          (fn [p] [{:name "Default"
                    :template (get-in p [:state :page-template])}])))

(defn get-categories
  "Retrieve categories for provider by applying categories function"
  [provider]
  ((category-function provider) provider))

(defn with-category
  "Modify provider, so we know what category we are currently
   working in and what it's template for this category."
  [provider {name :name template :template}]
  (-> provider
      (assoc-in [:state :page-template] template)
      (assoc-in [:state :category] name)
      (assoc-in [:state :current-val] (get-in provider [:state :init-val]))))


(defn done?
  "Is provider in done state?"
  [provider]
  (get-in provider [:state :done]))

(defn strategy [provider]
  (get-in provider [:configuration :strategy]))

(defn current-page [provider] ;; FIXME hack for rozetka
  (cond
    (and (= 1 (get-in provider [:state :current-val]))
         (get-in provider [:configuration :do-not-use-number-for-first-page]))
    (.replaceAll
      (get-in provider [:state :page-template])
      (get-in provider [:configuration :template-variable]) "")

    :else (format (get-in provider [:state :page-template])
                  (get-in provider [:state :current-val]))))

(defn category-name [provider]
  (get-in provider [:state :category] "default"))

(defn validate-configuration [provider]
  (let [known-str          #{:heavy :light :api}
        strategy           (get-in provider [:configuration :strategy])
        node->document     (get-in provider [:configuration :node->document])
        link-selector      (get-in provider [:configuration :link-selector])
        node-selector      (get-in provider [:configuration :node-selector])
        api-fn             (get-in provider [:configuration :api-fn])]

    (assert (known-str strategy) (str "Strategy must be one of " known-str))

    ;; validate heavy
    (when (= strategy :heavy)
      (log/trace "Strategy is :heavy")
      (assert node->document "node->document must be provided")
      (assert link-selector "URL Selector must be provided")
      (assert node-selector "Node Selector must be provided"))

    ;; validate light
    (when (= strategy :light)
      (log/trace "Strategy is :light")
      (assert node->document "node->document must be provided")
      (assert node-selector "Node selector must be provided"))

    (when (= strategy :api)
      (log/trace "Strategy is :api")
      (assert api-fn "api-fn must be provided"))))



(defn link-selector [provider]
  (get-in provider [:configuration :link-selector]))

(defn node-selector [provider]
  (get-in provider [:configuration :node-selector]))

(defn link-selector-relative? [provider]
  (not= :full-href (get-in provider [:configuration :link-selector-type])))

(defn last-page-selector [provider]
  (get-in provider [:configuration :last-page-selector]))

(defn node->document [provider]
  (get-in provider [:configuration :node->document]))

(defn query-api-fn [provider]
  (get-in provider [:configuration :api-fn]))

(defn heavy? [provider]
  (= :heavy (get-in provider [:configuration :strategy])))

(defn api? [provider]
  (= :api (get-in provider [:configuration :strategy])))

(defn light? [provider]
  (= :light (get-in provider [:configuration :strategy])))

(defn custom? [provider]
  (= :custom (get-in provider [:configuration :strategy])))

(defn get-page-template
  "Retrieves page-template from provider"
  [provider]
  (get-in provider [:state :page-template]))

(defn get-page-processed
  "Retrieves page-processed from provider"
  [provider]
  (get-in provider [:state :page-processed]))

(defn get-page-current
  "Retrieves page-processed from provider"
  [provider]
  (get-in provider [:state :page-current]))

(defn get-page-limit
  "Retrieves page-processed from provider"
  [provider]
  (get-in provider [:state :page-limit]))

(defn get-page->urls
  "Retrieves page-processed from provider"
  [provider]
  (get-in provider [:functions :page->urls]))

(defn get-last-page?
  "Retrieves page-processed from provider"
  [provider]
  (get-in provider [:functions :last-page?]))

(defn pname
  "Retrieves provider name"
  [provider]
  (get-in provider [:info :name]))

;;; Predicates

(defn limit-reached?
  "Returns true if provider reaches a limit of processed pages"
  [provider]
  (>= (get-page-processed provider)
      (get-page-limit provider)))



;;; Setters

(defn set-done
  "Set provider state to done"
  [provider]
  (assoc-in provider [:state :done] true))


(defn set-done-if-limit-reached
  [provider]
  (cond
    (limit-reached? provider) (set-done provider)
    :else                     provider))

(defn set-done-if-last-page
  [provider last-page-num]
  (cond
    (> (get-in provider [:state :page-current]) last-page-num) (set-done provider)
    :else provider))

(defn threads [provider]
  (get-in provider [:configuration :threads] 1))

(defn custom-process-fn [provider]
  (get-in provider [:configuration :custom-process-fn]))
