(ns org.healthsciencessc.rpms2.consent-collector.core
  (:require [org.healthsciencessc.rpms2.consent-collector.lock :as lock]
            [org.healthsciencessc.rpms2.consent-collector.process.authorize :as auth]
            [org.healthsciencessc.rpms2.process-engine.endpoint :as ws]
            [org.healthsciencessc.rpms2.process-engine.util :as util]
            
            [compojure.handler :as handler]
            [hiccup.middleware :as hicware]
            [ring.middleware.content-type :as content-type]
            [sandbar.stateful-session :as sandbar]
            
            [org.healthsciencessc.rpms2.consent-collector.process.init]))



(def app (-> 
           (ws/ws-constructor
             lock/lock-handler
             auth/ensure-auth-handler
             sandbar/wrap-stateful-session) ;; Enable session handling via sandbar
           (util/wrap-resource "public")    ;; Make resources/public items in search path
           content-type/wrap-content-type   
           hicware/wrap-base-url
           handler/site))

(defn init 
  "Initializes the application when it is first started up"
  []
  (util/bootstrap-addons "/consent/collect/bootstrap.clj"))
