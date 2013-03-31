(ns org.healthsciencessc.consent.collector.process.authorize
  (:require [org.healthsciencessc.consent.collector.respond :as respond]
            [org.healthsciencessc.consent.collector.state :as state]
            [org.healthsciencessc.consent.client.core :as services]
            [org.healthsciencessc.consent.domain.roles :as roles]
            [org.healthsciencessc.rpms2.process-engine.path :as path]
            [org.healthsciencessc.rpms2.process-engine.util :as util]
            
            [ring.util.response :as response])
  (:use     [pliant.process :only [defprocess]]))


(defprocess authenticate
  "Generates a view of the form designer for a single protocol version"
  [username password]
  (state/reset)
  (if-let [user (services/authenticate username password)]
    (cond
      (= :invalid user) 
        :invalid-user
      (roles/consent-collector? user)
        (do
          (state/set-user user)
          user)
      :else 
        :invalid-role)))

(defprocess is-authenticated?
  []
  (let [{:keys [username password]} (state/get-user)]
    (and username password)))

(defprocess is-authorized?
  []
  (let [{:keys [username password] :as user} (state/get-user)
        location (state/get-location)]
   (and username password (roles/consent-collector? user :location {:id (:id location)}))))

(defn ensure-auth-handler
  [handler]
  (fn [request]
    (let [path (util/path request)]
      (if (or (is-authenticated?)
              (= "/login" path)
              (= "/security/login" path))
        (handler request)
        (respond/redirect request "/login")))))