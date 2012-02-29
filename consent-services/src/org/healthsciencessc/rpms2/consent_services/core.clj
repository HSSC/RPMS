(ns org.healthsciencessc.rpms2.consent-services.core
  (:use compojure.core, ring.adapter.jetty)
  (:require [compojure.route :as route]))

(defroutes service-routes
  
  (GET "/*" [_ :as request]
       (println request)
       {:status 200
        :body "<h1>Hello World!</h1>"}))

(defn -main [& args] (run-jetty service-routes {:port 3000}))


