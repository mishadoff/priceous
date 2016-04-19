(ns priceous.rozetka
  (:require [clj-webdriver.taxi :as web]
            [clj-webdriver.core :as c]
            [taoensso.timbre :as log]
            [priceous.flow :as flow]
            [priceous.utils :as u]
            ))

(defn- price-from-element [e]
  (-> e
      (web/text)
      (clojure.string/trim)
      (clojure.string/replace #"[^0-9\\.]" "")
      (Double/parseDouble)
      ((fn [e] (if-not e (u/debug e) e)))
      ))

(defrecord RozetkaFlow [context]
  flow/IFlow

  (select-all-items-from-page [_]
    (u/selenium-failsafe-apply
     context
     true
     #(-> (web/find-elements {:class "g-i-tile-i-box-desc"}))
     "class: g-i-tile-i-box-desc"))

  (select-name-from-item [_ item]
    (u/selenium-failsafe-apply
     context
     true
     #(-> (web/find-element-under item {:class "g-i-tile-i-title clearfix"})
          (web/find-element-under {:tag :a :index 0})
          (web/text)
          (clojure.string/trim))
     "class: g-i-tile-i-title > a[0] > text"))

  (select-image-from-item [_ item]
    (u/selenium-failsafe-apply
     context
     true
     #(-> (web/find-element-under item {:class "g-i-tile-i-image fix-height"})
          (web/find-element-under {:tag "img"})
          (web/attribute "src"))
     "class: g-i-tile-i-image > tag: img > attr: src"))

  (select-link-from-item [_ item]
    (u/selenium-failsafe-apply
     context
     true
     #(-> (web/find-element-under item {:class "g-i-tile-i-title clearfix"})
          (web/find-element-under {:tag :a :index 0})
          (c/attribute "href"))
     "class: g-i-tile-i-title > a[0] > attr: href"))

  (select-price-from-item [_ item]
    (u/selenium-failsafe-apply
     context
     true
     #(-> (web/find-element-under item {:class "g-price-uah"})
          (price-from-element))
     "class: g-price-uah > text"))

  (select-old-price-from-item [_ item]
    (u/selenium-failsafe-apply
     context
     false
     #(-> (web/find-element-under item {:class "g-price-old-uah"})
          (price-from-element))
     "class: g-price-old-uah > text"))

  (select-sale-from-item [_ item]
    (u/selenium-failsafe-apply
     context
     false
     #(-> (web/find-element-under item {:class "g-price-old-uah"})
          (boolean))
     "class: g-price-old-uah"))

  (valid-element? [this item]
    (and
     ;; name is available
     (not (empty? (flow/select-name-from-item this item)))

     ;; unavailable status is not present
     (not (u/selenium-failsafe-apply
           context
           false
           #(-> (web/find-element-under item {:class "g-i-status unavailable"})
                (boolean))
           "class: g-i-status unavailable"))))
  
  (page-template [this]
    (:template context))

  (context [this] context)
  
  )

(def flow
  (let [template "http://rozetka.com.ua/krepkie-napitki/c4594292/filter/page=%s;vid-napitka-69821=whiskey-blend,whiskey-bourbon,whiskey-single-malt/"]
    ;; configurable
    (->RozetkaFlow {:provider "Rozetka"
                    :template template
                    :base (format template 1)})))
