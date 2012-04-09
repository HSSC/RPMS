(ns org.healthsciencessc.rpms2.consent-collector.i18n
  (:use [clojure.tools.logging :only (info)])
  (:require [j18n.core :as j18n]))

(def ^:private my-bundle (java.util.ResourceBundle/getBundle "org/healthsciencessc/rpms2/consent_collector/j18n"))

(defn- resource 
  "Returns the resource string associated with the message if 
   found in the bundle."
  [message bundle]
  (or (j18n/resource message bundle)
      (do
        (info "missing resource definition: " message)
        (name message))))


(defn- resource-existing 
  "Returns the resource string associated with the message if 
   found in the bundle."
  [message bundle]
  (j18n/resource message bundle))

(defn i18n-existing
  "Returns resource string associated with the message."
  [message]
  (resource-existing message my-bundle))

(defn i18n
  "Returns resource string associated with the message."
  [message]
  (resource message my-bundle))
