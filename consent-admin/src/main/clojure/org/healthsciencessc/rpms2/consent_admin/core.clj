(ns org.healthsciencessc.rpms2.consent-admin.core
  (:require [org.healthsciencessc.rpms2.process-engine [core :as pe]
                                                       [web-service :as ws]]
            [ring.util [codec :as codec]
                       [response :as response]]
            [org.healthsciencessc.rpms2.consent-admin.security :as security]
            [sandbar.stateful-session :as sandbar]
            [noir.validation :as validation]
            [ring.middleware.content-type :as content-type])
  (:use [compojure.core]
        [compojure.handler]
        [hiccup.middleware]
        [clojure.pprint]
        [org.healthsciencessc.rpms2.consent-admin.config]))


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

(defn wrap-debug
  "Simple middleware to print out everything.
  Can be placed in multiple points in the handling."
  [handler]
  (fn [request]
    (pprint request)
    (handler request)))

;; Enable session handling via sandbar 
;; Make resources/public items in search path
(def app (-> (ws/ws-constructor
               security/ensure-auth-handler
               sandbar/wrap-stateful-session)
             (wrap-resource "public")
             content-type/wrap-content-type
             wrap-base-url
             validation/wrap-noir-validation
           site))

(defn init 
  "Initializes the application when it is first started up"
  []
  (pe/load-processes (first (bootstrap-locations))))
