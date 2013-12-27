(ns org.healthsciencessc.consent.client.whoami
  "Functions for managing authentication identity."
  (:require [org.healthsciencessc.consent.client.session :as sess])
  (:use [pliant.process :only [defprocess]]))


(defprocess put-identity!
  "Adds the authenticated identity for services calls to the client session for reuse."
  [id]
  (sess/assoc-to-session! :identity id))


(defprocess get-identity
  "Gets the identity that was used to authenticate the current user."
  []
  (sess/get-in-session :identity))

(defprocess put-user!
  "Adds the the current user to the session."
  [user]
  (sess/assoc-to-session! :user user))

(defprocess get-user
  "Gets the current user from the session."
  []
  (sess/get-in-session :user))

(defprocess identified?
  "Checks if the current session has an authenticated identity and user associated with it."
  []
  (and (not (nil? (get-identity)))
       (not (nil? (get-user)))))

(defprocess deidentify!
  "Removes any authenticated information from the session, which should drop all session information."
  []
  (sess/dissoc-from-session! :identity :user))
