(defproject org.healthsciencessc.rpms2/consent-services "1.0.0-SNAPSHOT"
  :description "Consent Services API"
  :omit-default-repositories true
  :ring {:handler org.healthsciencessc.rpms2.consent-services.core/app
         :init org.healthsciencessc.rpms2.consent-services.core/ws-init}
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/data.json "0.1.2"]
                 [compojure "1.0.1"]
                 [org.clojure/data.codec "0.1.0"]
                 [clojurewerkz/neocons "1.0.0-beta1"]
                 [org.mindrot/jbcrypt "0.3m"]
                 [org.healthsciencessc.rpms2/process-engine "1.0.0-SNAPSHOT"]
                 [org.healthsciencessc.rpms2/consent-domain "1.0.0-SNAPSHOT"]]
  :dev-dependencies [[lein-ring "0.5.4"]
                     [ring-serve "0.1.2"]])
