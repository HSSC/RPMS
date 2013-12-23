(ns org.healthsciencessc.consent.commander.core
  (:require [pliant.configure.runtime :as runtime]
            [pliant.webpoint.request :as request]
            
            [sandbar.stateful-session :as sandbar]
            [org.healthsciencessc.consent.commander.security :as security]
            [ring.middleware.content-type :as content-type]
            [hiccup.middleware :as hicware]
            [org.healthsciencessc.consent.commander.process.init]
            
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
             security/ensure-auth-handler
             hicware/wrap-base-url
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
  (runtime/load-resources "consent/manager-bootstrap.clj"))
