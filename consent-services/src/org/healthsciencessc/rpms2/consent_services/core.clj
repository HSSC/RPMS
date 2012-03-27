(ns org.healthsciencessc.rpms2.consent-services.core
  (:use compojure.core)
  (:require [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-services.config :as config]
            [org.healthsciencessc.rpms2.consent-services.auth :as auth]
            [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.process-engine.web-service :as process-ws]))

(defn ws-init
  []
  (data/connect!)
  (process/load-processes (config/conf "default-process-directory")))

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
  (process-ws/ws-constructor (fn [handler]
                               (auth/wrap-authentication handler auth/authenticate))))
