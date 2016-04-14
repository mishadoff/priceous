(defproject priceous "0.0.1"
  :description "Process to monitor prices for precious items"
  :url "http://mishadoff.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]

                 ;; selenium
                 [clj-webdriver "0.7.2"]
                 [org.seleniumhq.selenium/selenium-server "2.47.0"]
                 
                 ;; logger
                 [com.taoensso/timbre "4.3.1"]
                 
                 ;; solr clienr
                 [com.codesignals/flux "0.6.0"]

                 ]
  :aot  [priceous.core]
  :main priceous.core
  )
