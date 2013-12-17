(defproject org.healthsciencessc.consent/services "1.0.0-SNAPSHOT"
  :description "Commander application for administration and design of consents."
  
  :source-paths ["src/main/clojure"]
  :test-paths ["src/test/clojure"]
  :resource-paths ["src/main/resources"]
  
  ;; Keep java source and project definition out of the artifact
  :jar-exclusions [#"^\." #"^*\/\." #"\.java$" #"project\.clj"]

  :plugins [[lein-package "2.1.1"]
            [lein-ring "0.8.8"]]
  
  :package {:autobuild false :reuse false :skipjar true
            :artifacts [{:build "ring uberwar" :extension "war" :classifier "standalone"}]}
  
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.healthsciencessc.consent/common "1.0.0-SNAPSHOT"]
                 [pliant/process "0.1.2"]
                 [pliant/configure "0.1.2"]
                 [pliant/webpoint "0.1.1"]
                 
                 [org.neo4j/neo4j "1.6.1"]
                 [borneo "0.3.0"]
                 [clojurewerkz/neocons "1.0.0-beta2"]
                 
                 [org.clojure/data.codec "0.1.0"]
                 [org.clojure/data.xml "0.0.4"]
                 [org.mindrot/jbcrypt "0.3m"]
                 
                 [ring/ring-servlet "1.2.1"]
                 [javax.servlet/servlet-api "2.5"]
                 
                 [log4j "1.2.16"]]
  
  :ring {:handler org.healthsciencessc.consent.services.core/app
         :init org.healthsciencessc.consent.services.core/init
         :destroy org.healthsciencessc.consent.services.core/destroy
         :port 8080}
  
  :profiles {:local    {:resource-paths ["local"]}
             :provided {:dependencies [[javax.servlet/servlet-api "2.5"]]}
             :dev      {:dependencies [[javax.servlet/servlet-api "2.5"]]}})