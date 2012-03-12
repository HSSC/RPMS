(ns org.healthsciencessc.rpms2.consent-services.core
  (:use compojure.core)
  (:require [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-services.auth :as auth]
            [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.process-engine.web-service :as process-ws]))

(defroutes development
  (GET "/reset-processes"
       []
       (do
         (reset! process/default-processes [])
         (reset! process/custom-processes [])
         (process/load-processes "resources/process_definitions"))
       "Done"))

(defroutes app
  development
  (-> process-ws/processes
      auth/wrap-authentication))

;; (data/connect)
(process/load-processes "resources/process_definitions")