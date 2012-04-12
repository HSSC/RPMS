(ns org.healthsciencessc.rpms2.consent-services.core
  (:use compojure.core)
  (:require [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-services.config :as config]
            [org.healthsciencessc.rpms2.consent-services.auth :as auth]
            [org.healthsciencessc.rpms2.consent-services.seed :as seed]
            [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.process-engine.web-service :as process-ws]))

(defn ws-init
  []
  (data/connect! (config/conf "neo4j-db-path"))
  (seed/setup-default-schema!)
  (process/load-processes config/default-process-class-path))

(defn ws-destroy
  []
  (data/shutdown!))

(defroutes development
  (GET "/reset-processes"
       []
       (do
         (reset! process/default-processes [])
         (reset! process/custom-processes [])
         (process/load-processes config/default-process-class-path))
       "Done"))

(defroutes app
  development
  (process-ws/ws-constructor (fn [handler]
                               (auth/wrap-authentication handler auth/authenticate))))
