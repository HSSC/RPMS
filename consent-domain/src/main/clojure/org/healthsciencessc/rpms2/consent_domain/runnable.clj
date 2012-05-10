;; Provides the reusable runnable? functions that work off the domain functions.
(ns org.healthsciencessc.rpms2.consent-domain.runnable
  (:require [org.healthsciencessc.rpms2.consent-domain.roles :as roles]))

(defn gen-designer-location-check
  "Creates a runnable? function that checks if a user is a protocol designer for a specific location."
  [userfn]
  (fn [ctx]
    (roles/protocol-designer? (userfn ctx) :location {:id (get-in ctx :query-params :location)})))

(defn gen-collector-location-check
  "Creates a runnable? function that checks if a user is a consent collector for a specific location."
  [userfn]
  (fn [ctx]
    (roles/consent-collector? (userfn ctx) :location {:id (get-in ctx :query-params :location)})))