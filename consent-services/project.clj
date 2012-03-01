(defproject org.healthsciencessc.rpms2/consent-services "1.0.0-SNAPSHOT"
  :description "Consent Services API"
  :omit-default-repositories true
  :ring {:handler org.healthsciencessc.rpms2.consent-services.core/app}
  :main org.healthsciencessc.rpms2.consent-services.core
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [ring/ring-jetty-adapter "1.1.0-SNAPSHOT"]
                 [compojure "1.0.1"]]
  :dev-dependencies [[lein-ring "0.5.4"]])