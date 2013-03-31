(ns org.healthsciencessc.consent.manager.core
  (:require [org.healthsciencessc.rpms2.process-engine [util :as util]
                                                       [endpoint :as ws]]
            [sandbar.stateful-session :as sandbar]
            [org.healthsciencessc.consent.manager.security :as security]
            [ring.middleware.content-type :as content-type]
            [hiccup.middleware :as hicware]
            [compojure.handler :as handler]
            [org.healthsciencessc.consent.manager.process.init]))



(def app (-> 
           (ws/ws-constructor
             security/ensure-auth-handler
             sandbar/wrap-stateful-session) ;; Enable session handling via sandbar
           (util/wrap-resource "public")    ;; Make resources/public items in search path
           content-type/wrap-content-type   
           hicware/wrap-base-url
           handler/site))

(defn init 
  "Initializes the application when it is first started up"
  []
  (util/bootstrap-addons "/rpms/admin/bootstrap.clj"))
