(defproject org.healthsciencessc.consent/common "1.0.0-SNAPSHOT"
  :description "Project for common data structures and functions."
  
  :source-paths ["src/main/clojure"]
  :test-paths ["src/test/clojure"]
  
  ;; Keep java source and project definition out of the artifact
  :jar-exclusions [#"^\." #"^*\/\." #"\.java$" #"project\.clj"]

  :plugins [[lein-package "0.1.1"]]
  
  :hooks [leiningen.package.hooks.deploy 
          leiningen.package.hooks.install]
  
  :package {:autobuild false :reuse false}
  
  :dependencies [[org.clojure/clojure "1.4.0"]])
