(ns org.healthsciencessc.rpms2.consent-admin.core
  (:require [org.healthsciencessc.rpms2.process-engine [core :as pe]
                                                       [web-service :as ws]]
            [ring.util [codec :as codec]
                       [response :as response]])
  (:use compojure.core)
  (:use org.healthsciencessc.rpms2.consent-admin.config)
  (:use [sandbar.stateful-session :only [session-put!
                                         session-get
                                         session-delete-key!
                                         flash-put!
                                         flash-get]])
  (:require [org.healthsciencessc.rpms2.consent-admin.process [login :as login]]))


;;(process/load-processes (first (bootstrap-locations)))

(defn wrap-resource
  "Middleware that first checks to see whether the request map matches a static
  resource. If it does, the resource is returned in a response map, otherwise
  the request map is passed onto the handler. The root-path argument will be
  added to the beginning of the resource path."
  [handler root-path]
  (fn [request]
    (if-not (= :get (:request-method request))
      (handler request)                        
      (let [path (.substring (codec/url-decode (:path-info request)) 1)]
        (or (response/resource-response path {:root root-path})
                        (handler request))))))

;; Enable session handling via sandbar 
;; Make resources/public items in search path
(def app (-> (ws/ws-constructor)
             (sandbar.stateful-session/wrap-stateful-session)
             (wrap-resource "public")))