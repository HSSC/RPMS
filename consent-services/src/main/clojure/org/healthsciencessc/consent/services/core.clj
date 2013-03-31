(ns org.healthsciencessc.consent.services.core
  (:require [org.healthsciencessc.consent.services.auth :as auth]
            [org.healthsciencessc.consent.services.config :as config]
            [org.healthsciencessc.consent.services.data :as data]
            [org.healthsciencessc.consent.services.seed :as seed]
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint]
            [org.healthsciencessc.rpms2.process-engine.util :as util]
            [org.healthsciencessc.consent.services.process.init]))

(defn init
  []
  (data/connect! (config/conf "neo4j-db-path"))
  (seed/seed)
  (util/bootstrap-addons))

(defn destroy
  []
  (data/shutdown!))

(def app
  (endpoint/ws-constructor (fn [handler] (auth/wrap-authentication handler auth/authenticate))))
