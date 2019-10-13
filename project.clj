(defproject priceous "0.3.0"
  :description "Price aggregator for alcohol"
  :url "http://priceous.mishadoff.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]

                 ;; Instrumenting and dynamic providers loading
                 [org.clojure/tools.namespace "0.3.1"]

                 ;; server and middleware
                 [ring "1.7.1"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-json "0.5.0"]

                 ;; routing
                 [compojure "1.6.1"]

                 ;; templating
                 [hiccup "1.0.5"]

                 ;; for api based scrapping
                 [clj-http "3.10.0"]

                 ;; for selector based scrapping
                 [enlive "1.1.6"]

                 ;; logger
                 [com.taoensso/timbre "4.10.0"]
                 [com.fzakaria/slf4j-timbre "0.3.14"]

                 ;; Components management
                 [integrant "0.7.0"]

                 ;; Prismatic schema
                 [prismatic/schema "1.1.12"]

                 ;; solr client
                 ;; FIXME fork of com.codesignals/flux
                 [com.mishadoff/flux "0.6.1"]

                 ;; good times
                 [clj-time "0.15.2"]

                 ;; we also can write to csv
                 [org.clojure/data.csv "0.1.4"]

                 ;; json is a must
                 [cheshire "5.9.0"]

                 ;; Rate limiter per IP / revisit
                 [ring-ratelimit "0.2.2"]

                 ;; under dmi3iy influence
                 [org.clojure/test.check "0.10.0"]

                 ;; extract version of the project into app
                 [trptcolin/versioneer "0.2.0"]

                 ;; Mail library for alerts
                 [com.draines/postal "2.0.3"]]

                 ;; Selenium

  :aliases {"goodwine" ["run" "-m" "priceous.core" "goodwine"]}

  :repl-options {:init-ns user}

  :plugins [[lein-ancient "0.6.15"]]

  :source-paths ["src" "dev"]

  :profiles {:dev {:plugins [[jonase/eastwood "0.2.1"]
                             [lein-kibit "0.1.2"]
                             [lein-bikeshed "0.2.0"]
                             [lein-cloverage "1.0.6"]]}}

  ;; no opts for now
  :jvm-opts []

  :uberjar-name "priceous.jar"

  :aot  [priceous.main]
  :main priceous.main
  )

