(ns org.healthsciencessc.consent.collector.state
  (:require [org.healthsciencessc.consent.client.session :as sess]
            [org.healthsciencessc.consent.client.core :as services]
            [org.healthsciencessc.consent.client.whoami :as whoami])
  (:use     [pliant.process :only [defprocess]]))


(defprocess reset-consent-session
  []
  (sess/dissoc-from-session! :consent-session))


(defprocess get-organization
  []
  (:organization (whoami/get-user)))


(defprocess set-location
  [location]
  (if (:id location)
    (sess/assoc-to-session! :location location)
    (sess/assoc-to-session! :location (services/get-location location)))
  (sess/dissoc-from-session! :consent-session))

(defprocess get-location
  []
  (sess/get-in-session :location))

;; Consent Session
(defprocess get-consent-session
  []
  (sess/get-in-session :consent-session))

(defprocess in-session?
  []
  (if (get-consent-session) true false))

(defprocess set-consenter
  "Sets the ID of the consenter consents are being collected against."
  [consenter]
  (sess/assoc-in-session! [:consent-session :consenter] consenter))

(defprocess get-consenter
  "Gets the ID of the consenter consents are being collected against."
  []
  (sess/get-in-session :consent-session :consenter))

(defprocess set-encounter
  "Sets the ID of the encounter consents are being collected against."
  [encounter]
  (sess/assoc-in-session! [:consent-session :encounter] encounter))

(defprocess get-encounter
  "Gets the ID of the encounter consents are being collected against."
  []
  (sess/get-in-session :consent-session :encounter))

(defprocess set-protocols
  "Puts the IDs of the protocols and language chosen in the consenting process to make available for 
   downstream processing.  Small footprint."
  [protocols language]
  (sess/assoc-in-session! [:consent-session :protocols] protocols)
  (sess/assoc-in-session! [:consent-session :language] language))

(defprocess get-protocols
  "Gets the IDs of the protocols chosen."
  []
  (sess/get-in-session :consent-session :protocols))

(defprocess get-protocol-language
  "Gets the ID of the language chosen."
  []
  (sess/get-in-session :consent-session :language))

;; Session State Reporting - For Debugging Purposes
#_(defprocess all
   []
   {:identity (whoami/get-identity)
    :user (whoami/get-user)
    :location (get-location)
    :consent-session (get-consent-session)})
