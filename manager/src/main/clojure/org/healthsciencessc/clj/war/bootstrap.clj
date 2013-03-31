(ns org.healthsciencessc.clj.war.bootstrap
  (:require [org.healthsciencessc.consent.manager.core :as core]))

;; Bootrap a pointer to the real routes.
(def app core/app)

(defn init
  "Provides a function placeholder to initialize the application from."
  [event]
  (core/init))

(defn destroy
  "Provides a function placeholder to perform destroy operations whent the servlet is shutdown."
  [event]
  (println "Destroying the Consent Admin Application."))