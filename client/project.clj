(defproject org.healthsciencessc.consent/client "1.0.0-SNAPSHOT"
  :description "Provides a simple reusable client interface for calling the consent services."
  
  :source-paths ["src/main/clojure"]
  :test-paths ["src/test/clojure"]
  
  ;; Keep java source and project definition out of the artifact
  :jar-exclusions [#"^\." #"^*\/\." #"\.java$" #"project\.clj"]

  :plugins [[lein-package "2.1.1"]]
  
  :package {:autobuild false :reuse false}
  
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.healthsciencessc.consent/common "1.0.0-SNAPSHOT"]
                 [pliant/configure "0.1.2"]
                 [clj-http "0.4.0"]
                 [sandbar "0.4.0-SNAPSHOT"]
                 [org.clojure/tools.logging "0.2.6"]])
