(ns org.healthsciencessc.consent.commander.security
  (:require [org.healthsciencessc.consent.client.whoami :as whoami]
            [pliant.webpoint.common :as common]
            [pliant.webpoint.url :as url]
            [ring.util.response :as response])
  (:use [compojure.core]))


(defn ensure-auth-handler
  [handler]
  (fn [request]
    (let [path (common/path request)]
      (if (or (whoami/identified?)
              (= "/login" path)
              (= "/security/login" path))
        (handler request)
        (response/redirect (url/root-link request "/login"))))))

(defn current-user
  "Method used to get the current user for the request.  Multi interface allows for use in common runnables functions."
  ([& ignore] (whoami/get-user)))

(defn current-org
  "Obtains the organization from the current user.  If out of session returns nil."
  [& ignore]
  (:organization (current-user)))

(defn current-org-id
  "Obtains the organization from the current user.  If out of session returns nil."
  [& ignore]
  (get-in (current-user) [:organization :id]))