(ns org.healthsciencessc.clj.war.bootstrap
  (:require [org.healthsciencessc.rpms2.consent-services.core :as core]))

;; Bootrap a pointer to the real routes.
(def app core/app)

(defn init
  "Provides a function placeholder to initialize the application from."
  [event]
  (core/ws-init))

(defn destroy
  "Provides a function placeholder to perform destroy operations whent the servlet is shutdown."
  [event]
  (core/ws-destroy)
  (println "Destroying the Consent Services Application."))
