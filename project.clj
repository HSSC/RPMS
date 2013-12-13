(defproject org.healthsciencessc.consent/root "1.0.0-SNAPSHOT"
  :description "Root project to manage the project modules."
  
  :plugins [[lein-package "2.1.1"]
            [lein-sub "0.3.0"]]
  
  :hooks [leiningen.package.hooks.deploy 
          leiningen.package.hooks.install]
  
  :sub ["common" "client" "services" "commander" "collector"])
