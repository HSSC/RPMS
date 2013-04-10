(ns org.healthsciencessc.consent.client.whoami
  "Functions for managing authentication identity."
  (:require [sandbar.stateful-session :as sess])
  (:use [pliant.process :only [defprocess]]))


(defprocess put-identity!
  [id]
  (sess/session-put! :identity id))

(defprocess get-identity
  []
  (sess/session-get :identity))

(defprocess identified?
  []
  (not (nil? (get-identity))))

(defprocess deidentify!
  []
  (sess/session-delete-key! :identity))