(ns org.healthsciencessc.consent.commander.core
  (:require [pliant.configure.runtime :as runtime]
            [pliant.webpoint.middleware :as webware]
            [sandbar.stateful-session :as sandbar]
            [org.healthsciencessc.consent.commander.security :as security]
            [ring.middleware.content-type :as content-type]
            [hiccup.middleware :as hicware]
            [compojure.handler :as handler]
            [org.healthsciencessc.consent.commander.process.init]))



(def app (-> 
           (webware/inject-routes
             security/ensure-auth-handler
             sandbar/wrap-stateful-session) ;; Enable session handling via sandbar
           (webware/wrap-resource "public")    ;; Make resources/public items in search path
           content-type/wrap-content-type   
           hicware/wrap-base-url
           handler/site))

(defn init 
  "Initializes the application when it is first started up"
  []
  (runtime/load-resources "consent/manager-bootstrap.clj"))
