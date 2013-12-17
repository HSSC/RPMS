(defproject org.healthsciencessc.consent/collector "1.0.0-SNAPSHOT"
  :description "The tablet-optimized web application for collecting consents."
  
  :source-paths ["src/main/clojure"]
  :resource-paths ["src/main/resources"]
  :test-paths ["src/test/clojure"]
  
  ;; Keep java source and project definition out of the artifact
  :jar-exclusions [#"^\." #"^*\/\." #"\.java$" #"project\.clj"]

  :plugins [[lein-package "2.1.1"]
            [lein-ring "0.8.8"]]
  
  :package {:autobuild false :reuse false :skipjar true
            :artifacts [{:build "ring uberwar" :extension "war" :classifier "standalone"}]}
  
  :ring {:handler org.healthsciencessc.consent.collector.core/app
         :init org.healthsciencessc.consent.collector.core/init
         :port 8081}
  
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.healthsciencessc.consent/common "1.0.0-SNAPSHOT"]
                 [org.healthsciencessc.consent/client "1.0.0-SNAPSHOT"]
                 [pliant/configure "0.1.2"]
                 [pliant/process "0.1.2"]
                 [pliant/webpoint "0.1.1"]
                 
                 [hiccup "1.0.0"]
                 
                 [ring/ring-servlet "1.2.1"]
                 [javax.servlet/servlet-api "2.5"]
                 
                 [log4j "1.2.16"]]
  
  :profiles {:local {:resource-paths ["local"]}})


