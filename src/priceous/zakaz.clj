(ns priceous.zakaz
  (:require [clj-webdriver.taxi :as web]
            [clj-webdriver.core :as c]
            [taoensso.timbre :as log]
            [priceous.flow :as flow]
            [priceous.utils :as u]
            ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Generic Package to Process zakaz.ua specific items ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- price-from-element [e]
  (Double/parseDouble
   (str (c/text (web/find-element-under e {:class "grivna price"}))
        "."
        (c/text (web/find-element-under e {:class "kopeiki"})))))



;;; Worker Object
;;; All zakaz-based providers can extend it
 
(defrecord ZakazFlow [context]
  flow/IFlow

  (select-all-items-from-page [_]
    (u/selenium-failsafe-apply
     context
     true
     #(web/find-elements {:class "one-product"})
     "class: one-product"))

  (select-name-from-item [_ item]
    (u/selenium-failsafe-apply
     context
     true
     #(->> (web/find-element-under item {:class "one-product-name"})
           (web/text)
           (clojure.string/trim))
     "class: one-product-name > text"))

  (select-link-from-item [_ item]
    (u/selenium-failsafe-apply
     context
     true
     #(-> (web/find-element-under item {:class "one-product-link"})
          (c/attribute "href"))
     "class: one-product-link > attr: href"))

  (select-image-from-item [_ item]
    (u/selenium-failsafe-apply
     context
     true
     #(-> item
          (web/find-element-under {:class "one-product-link"})
          (web/find-element-under {:class "one-product-image"})
          (web/find-element-under {:tag "img"})
          (web/attribute "src"))
     "class: one-product-link > class: one-product-image > tag: img > attr: src"))

  (select-price-from-item [_ item]
    (u/selenium-failsafe-apply
     context
     true
     #(-> item
          (web/find-element-under {:class "one-product-button"})
          (web/find-element-under {:class "one-product-price"})
          (price-from-element))
     "class: one-product-button > class: one-product-price > class: grivna price & kopeiki"))

  (select-old-price-from-item [_ item]
    (u/selenium-failsafe-apply
     context
     false
     #(-> item
          (web/find-element-under {:class "badge right-up-sale-bage"})
          (price-from-element))
     "class: badge right-up-sale-bage > class: grivna price & kopeiki"))

  (select-sale-from-item [_ item]
    (u/selenium-failsafe-apply
     context
     false
     #(-> item
          (web/find-element-under {:class "badge right-up-sale-bage"})
          (boolean))
     "class: badge right-up-sale-bage"))

  (valid-element? [this item]
    (not (empty? (flow/select-name-from-item this item))))

  (page-template [this]
    (str (:base context) "/?&page=%s"))

  (context [this] context)
  
  )
