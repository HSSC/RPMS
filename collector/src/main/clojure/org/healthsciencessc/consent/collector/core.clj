(ns org.healthsciencessc.consent.collector.core
  (:require [org.healthsciencessc.consent.collector.lock :as lock]
            [org.healthsciencessc.consent.collector.process.authorize :as auth]
            
            [pliant.configure.runtime :as runtime]
            [pliant.webpoint.middleware :as webware]
            [pliant.webpoint.request :as webquest]
            
            [compojure.handler :as handler]
            [hiccup.middleware :as hicware]
            [ring.middleware.content-type :as content-type]
            [sandbar.stateful-session :as sandbar]
            
            [org.healthsciencessc.consent.collector.process.init]))



(def app (-> 
           (webware/inject-routes
             lock/lock-handler
             auth/ensure-auth-handler
             sandbar/wrap-stateful-session) ;; Enable session handling via sandbar
           (webware/wrap-resource "public")    ;; Make resources/public items in search path
           content-type/wrap-content-type   
           hicware/wrap-base-url
           handler/site))

(defn init 
  "Initializes the application when it is first started up"
  []
  (runtime/load-resources "consent/collect-bootstrap.clj"))
