(ns org.healthsciencessc.consent.collector.state
  (:require [sandbar.stateful-session :as sandbar]
            [org.healthsciencessc.consent.client.core :as services]
            [org.healthsciencessc.consent.client.whoami :as whoami])
  (:use     [pliant.process :only [defprocess]]))

;; Flash Functions - State That Lives For One Request

(defn- flash
  []
  (or (sandbar/session-get :_app-flash) {}))

(defn- flash!
  [m]
  (sandbar/session-put! :_app-flash m))

(defprocess flash-put!
  [k v]
  (flash! (assoc (flash) k v)))

(defprocess flash-get
  [k]
  (let [m (flash)
        v (m k)]
    (flash! (dissoc m k))
    v))

;; Reset Functions
(defprocess reset
  []
  (sandbar/destroy-session!))

(defprocess reset-consent-session
  []
  (sandbar/session-delete-key! :consent-session))


;; Application State Methods
#_(defprocess set-user
   [user]
   (sandbar/session-put! :user user))

#_(defprocess get-user
   []
   (sandbar/session-get :user))

(defprocess get-organization
  []
  (:organization (sandbar/session-get :user)))

(defprocess set-location
  [location]
  (if (:id location)
    (sandbar/session-put! :location location)
    (sandbar/session-put! :location (services/get-location location)))
  (reset-consent-session))

(defprocess get-location
  []
  (sandbar/session-get :location))

;; Consent Session
(defprocess get-consent-session
  []
  (sandbar/session-get :consent-session))

(defprocess in-session?
  []
  (if (get-consent-session) true false))

(defprocess set-consenter
  [consenter]
  (sandbar/session-put! :consent-session {:consenter consenter}))

(defprocess get-consenter
  []
  (:consenter (get-consent-session)))

(defprocess set-encounter
  [encounter]
  (sandbar/session-put! :consent-session (merge (get-consent-session) {:encounter encounter})))

(defprocess get-encounter
  []
  (:encounter (get-consent-session)))

(defprocess set-protocols
  [protocols language]
  (sandbar/session-put! :consent-session (merge (get-consent-session) {:protocols protocols
                                                                       :language language})))
(defprocess get-protocols
  []
  (:protocols (get-consent-session)))

(defprocess get-protocol-language
  []
  (:language (get-consent-session)))

;; Session State Reporting - For Debugging Purposes
(defprocess all
  []
  {:identity (whoami/get-identity)
   :user (whoami/get-user)
   :location (get-location)
   :consent-session (get-consent-session)})
