(defproject org.healthsciencessc.consent/commander "1.0.0-SNAPSHOT"
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
                 [org.healthsciencessc.consent/client "1.0.0-SNAPSHOT"]
                 [pliant/process "0.1.1"]
                 [pliant/configure "0.1.2"]
                 [pliant/webpoint "0.1.1"]
                 
                 [hiccup "1.0.0"]
                 [sandbar "0.4.0-SNAPSHOT"]
          
                 [ring/ring-servlet "1.2.1"]
                 [javax.servlet/servlet-api "2.5"]
                 
                 [log4j "1.2.16"]]
  
  :ring {:handler org.healthsciencessc.consent.commander.core/app
         :init org.healthsciencessc.consent.commander.core/init
         :port 8082}
  
  :profiles {:local {:resource-paths ["local"]}})
