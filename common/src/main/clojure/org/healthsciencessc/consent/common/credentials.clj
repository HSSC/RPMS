;; Provides the reusable functions for dealing with our tenancy.
(ns org.healthsciencessc.consent.common.credentials
  (:require [clojure.string]))

(defn wrap-username
  "Wraps"
  ([id] (if (map? id)
          (wrap-username (:username id) (:realm id))
          (wrap-username id "local")))
  ([username realm] (str "[" username "]@[" (or realm "local") "]")))

(defn wrap-password
  "Wraps the password in"
  [password]
  (cond 
    (nil? password) nil
    (map? password) (str "[" (:password password) "]")
    :else (str "[" password "]")))

(defn unwrap-credentials
  "Returns a map containing the realm"
  [credentials]
  (when credentials 
    {:realm (or (last (re-find #"@\[(.*)\]:" credentials))
                "local")
     :username (last (or (re-find #"^\[(.*)\]@" credentials)
                         (re-find #"^\[(.*)\]:" credentials)
                         (re-find #"^(.*):" credentials)))
     :password (last (or (re-find #":\[(.*)\]$" credentials)
                         (re-find #":(.*)$" credentials)))}))

(defn valid?
  "Returns a map containing the realm"
  [credentials]
  (and (:realm credentials)
       (:username credentials)
       (:password credentials)))
