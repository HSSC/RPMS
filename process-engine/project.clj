(defproject org.healthsciencessc.rpms2/process-engine "1.0.0-SNAPSHOT"
  :description "Process Engine For RPMS Applications"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [ring/ring-jetty-adapter "1.1.0-SNAPSHOT"]
                 [compojure "1.0.1"]
                 [org.clojure/tools.logging "0.2.3"]
                 [org.clojure/data.json "0.1.2"]
                 [slingshot "0.10.2"]]
  :dev-dependencies [[lein-ring "0.5.4"]
                     [ring-serve "0.1.2"]]
  :aot [org.healthsciencessc.rpms2.process-engine.core
        org.healthsciencessc.rpms2.process-engine.web-service])