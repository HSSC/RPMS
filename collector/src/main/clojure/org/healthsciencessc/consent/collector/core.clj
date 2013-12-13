(ns org.healthsciencessc.consent.collector.core
  (:require [org.healthsciencessc.consent.collector.lock :as lock]
            [org.healthsciencessc.consent.collector.process.authorize :as auth]
            
            [pliant.configure.runtime :as runtime]
            [pliant.webpoint.request :as request]
            
            [hiccup.middleware :as hiccup]
            [ring.middleware.content-type :as content-type]
            [sandbar.stateful-session :as sandbar]
            
            [org.healthsciencessc.consent.collector.process.init]
            
            [ring.middleware [params :refer [wrap-params]]
                             [keyword-params :refer [wrap-keyword-params]]
                             [nested-params :refer [wrap-nested-params]]
                             [session :refer [wrap-session]]
                             [flash :refer [wrap-flash]]]
            
            [pliant.webpoint [middleware :refer [wrap-keyify-params 
                                                 wrap-log-request 
                                                 wrap-resolve-body 
                                                 wrap-resource]]]))


(def app (-> request/route
             wrap-log-request
             wrap-resolve-body
             (wrap-keyify-params :query-params)
             lock/lock-handler
             auth/ensure-auth-handler
             hiccup/wrap-base-url
             wrap-keyword-params
             wrap-nested-params
             wrap-params
             sandbar/wrap-stateful-session
             wrap-flash
             wrap-session
             (wrap-resource "public")
             content-type/wrap-content-type))

(defn init 
  "Initializes the application when it is first started up"
  []
  (runtime/load-resources "consent/collect-bootstrap.clj"))
