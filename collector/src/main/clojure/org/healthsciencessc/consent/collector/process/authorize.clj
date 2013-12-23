(ns org.healthsciencessc.consent.collector.process.authorize
  (:require [org.healthsciencessc.consent.client.core :as services]
            [org.healthsciencessc.consent.client.whoami :as whoami]
            [org.healthsciencessc.consent.collector.respond :as respond]
            [org.healthsciencessc.consent.collector.state :as state]
            [org.healthsciencessc.consent.common.roles :as roles]
            [pliant.webpoint.common :as common]
            [ring.util.response :as response]
            [clojure.tools.logging :as logging])
  (:use     [pliant.process :only [defprocess]]))


(defprocess authenticate
  "Generates a view of the form designer for a single protocol version"
  ([username password] (authenticate username password "local"))
  ([username password realm]
    (state/reset)
    (services/authenticate username password realm roles/consent-collector?)))

(defprocess is-authorized?
  []
  (let [user (whoami/get-user)
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
        (do
          (logging/info "Not authenticated for path: " path)
          (respond/redirect request "/login"))))))