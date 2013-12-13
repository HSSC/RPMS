(ns org.healthsciencessc.consent.services.core
  (:require [org.healthsciencessc.consent.services.auth :as auth]
            [org.healthsciencessc.consent.services.config :as config]
            [org.healthsciencessc.consent.services.data :as data]
            [org.healthsciencessc.consent.services.upgrade :as upgrade]
            
            [pliant.configure.runtime :as runtime]
            [pliant.process :as process]
            [pliant.webpoint.request :as request]
            [pliant.webpoint.response :as response]
            
            [org.healthsciencessc.consent.services.process.init]
            
            [ring.middleware [params :refer [wrap-params]]
                             [keyword-params :refer [wrap-keyword-params]]
                             [nested-params :refer [wrap-nested-params]]
                             [session :refer [wrap-session]]]
            
            [pliant.webpoint [middleware :refer [wrap-keyify-params 
                                                 wrap-log-request 
                                                 wrap-resolve-body]]]))

(defn init
  "Initialize the services application."
  []
  (data/connect! (config/conf "neo4j-db-path"))
  (upgrade/check-version)
  (runtime/load-resources "consent/services-bootstrap.clj"))


(defn destroy
  "Shutdown the application."
  []
  (data/shutdown!))


(def app (-> request/route
          (auth/wrap-authentication auth/authenticate)
           wrap-log-request
           wrap-resolve-body
           (wrap-keyify-params :query-params)
           wrap-session
           wrap-keyword-params
           wrap-nested-params
           wrap-params))


(process/deflayer response/on-exception services-on-exception
  "Add a services specific exception handling layer."
  [exception request]
  (if-let [data (ex-data exception)]
    (if (= (:type (:object data)) :org.healthsciencessc.consent.services.data/invalid-record)
      (response/respond-in-error 
        {:errors (:errors (:object data)) :message "Provided data failed validation."} request 401)
      (process/continue))
    (process/continue)))
