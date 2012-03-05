(defproject org.healthsciencessc.rpms2/consent-services "1.0.0-SNAPSHOT"
  :description "Consent Services API"
  :omit-default-repositories true
  :ring {:handler org.healthsciencessc.rpms2.consent-services.core/app}
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [ring/ring-jetty-adapter "1.1.0-SNAPSHOT"]
                 [compojure "1.0.1"]
                 [org.clojure/data.json "0.1.2"]
                 [org.healthsciencessc.rpms2/process-engine "1.0.0-SNAPSHOT"]]
  :dev-dependencies [[lein-ring "0.5.4"]])