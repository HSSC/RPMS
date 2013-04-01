(ns org.healthsciencessc.consent.services.core
  (:require [org.healthsciencessc.consent.services.auth :as auth]
            [org.healthsciencessc.consent.services.config :as config]
            [org.healthsciencessc.consent.services.data :as data]
            [org.healthsciencessc.consent.services.seed :as seed]
            [pliant.configure.runtime :as runtime]
            [pliant.webpoint.middleware :as webware]
            [org.healthsciencessc.consent.services.process.init]))

(defn init
  []
  (data/connect! (config/conf "neo4j-db-path"))
  (seed/seed)
  (runtime/load-resources "consent/services-bootstrap.clj"))

(defn destroy
  []
  (data/shutdown!))

(def app
  (webware/inject-routes (fn [handler] (auth/wrap-authentication handler auth/authenticate))))
