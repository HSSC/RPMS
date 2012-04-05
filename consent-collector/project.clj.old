(defproject consent-collector "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.healthsciencessc.rpms2/process-engine "1.0.0-SNAPSHOT"]
                 [clj-http "0.3.2"]
                 [hiccup "0.3.8"]
                 [ring "1.0.2"]
                 [sandbar/sandbar "0.4.0-SNAPSHOT"]
                 [org.clojure/tools.logging "0.2.3"]
                 [org.clojure/data.json "0.1.2"]
                 [log4j/log4j "1.2.16"]
                 [j18n "1.0.0"]]
  :dev-dependencies [[lein-ring "0.5.4"]]
  :plugins [[lein-cljsbuild "0.1.2"]]
  :cljsbuild {
              :builds [{:source-path "src-cljs"
                        :compiler {:output-to "resources/public/app.js",
                                   :optimizations :whitespace
                                   :pretty-print true}}]}
  :ring {:handler org.healthsciencessc.rpms2.consent-collector.core/app}
  :aot [org.healthsciencessc.rpms2.consent-collector.core])
