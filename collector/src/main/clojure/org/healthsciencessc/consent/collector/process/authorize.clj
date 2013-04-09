(ns org.healthsciencessc.consent.collector.process.authorize
  (:require [org.healthsciencessc.consent.client.core :as services]
            [org.healthsciencessc.consent.client.whoami :as whoami]
            [org.healthsciencessc.consent.collector.respond :as respond]
            [org.healthsciencessc.consent.collector.state :as state]
            [org.healthsciencessc.consent.domain.roles :as roles]
            [pliant.webpoint.common :as common]
            [ring.util.response :as response])
  (:use     [pliant.process :only [defprocess]]))


(defprocess authenticate
  "Generates a view of the form designer for a single protocol version"
  ([username password] (authenticate username password "local"))
  ([username password realm]
    (state/reset)
    (if-let [user (services/authenticate username password realm)]
      (cond
        (= :invalid user) 
          :invalid-user
        (roles/consent-collector? user)
          (do
            (state/set-user user)
            user)
        :else 
          :invalid-role))))

(defprocess is-authorized?
  []
  (let [user (state/get-user)
        location (state/get-location)]
   (and (whoami/identified?) (roles/consent-collector? user :location {:id (:id location)}))))

(defn ensure-auth-handler
  [handler]
  (fn [request]
    (let [path (common/path request)]
      (if (or (whoami/identified?)
              (= "/login" path)
              (= "/security/login" path))
        (handler request)
        (respond/redirect request "/login")))))