(ns org.healthsciencessc.consent.client.session
  "Provides a single api for session state management."
  (:require [sandbar.stateful-session :as sandbar])
  (:use [pliant.process :only [defprocess]]))

;; Flash Session Functions
(defn- flash
  "Internal flash getter."
  []
  (or (sandbar/session-get :_app-flash) {}))

(defn- flash!
  "Internal flash setter."
  [m]
  (sandbar/session-put! :_app-flash m))

(defprocess flash-get
  "Gets a value that is destroyed after it is pulled the first time."
  [k]
  (let [m (flash)
        v (m k)]
    (flash! (dissoc m k))
    v))

(defprocess flash-put!
  "Adds a value to flash that will be removed on the first pull of that value."
  [k v]
  (flash! (assoc (flash) k v)))


;; Session Functions
(defn- session
  "Gets the application session map."
  []
  (or (sandbar/session-get :_app-session) {}))

(defn- session!
  "Sets the application session map with the value passed."
  [m]
  (sandbar/session-put! :_app-session m))

(defprocess get-in-session
  "Gets a value from the application session map."
  [& ks]
  (get-in (session) ks))

(defprocess assoc-to-session!
  "Assocs a value the the application session map."
  ([k v] (session! (assoc (session) k v)))
  ([k v & kvs] (session! (apply assoc (session) k v kvs))))

(defprocess assoc-in-session!
  "Assocs a value the the application session map at the path indicated."
  [ks v]
  (session! (assoc-in (session) ks v)))

(defprocess dissoc-from-session!
  "Removes a key value from the application session map."
  ([k] (session! (dissoc (session) k)))
  ([k & ks] (session! (apply dissoc (session) k ks))))

(defprocess destroy-session!
  "Resets/destroys the session."
  []
  (sandbar/destroy-session!))
