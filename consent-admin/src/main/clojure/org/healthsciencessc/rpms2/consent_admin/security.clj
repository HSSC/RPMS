(ns org.healthsciencessc.rpms2.consent-admin.security
  (:require ;;[org.healthsciencessc.rpms2.consent-admin.security :as security]
            [org.healthsciencessc.rpms2.process-engine.path :as path]
            [ring.util.response :as response]
            [sandbar.stateful-session :as sandbar])
  (:use [compojure.core]
        [clojure.pprint]
        [org.healthsciencessc.rpms2.consent-admin.config]))

(defn is-authenticated?
  []
  (let [{:keys [username password]} (sandbar/session-get :user)]
    (and username password)))

(defn ensure-auth-handler
  [handler]
  (fn [{:keys [path-info] :as request}]
    (if (or (is-authenticated?)
          (= "/login" path-info)
          (= "/security/login" path-info))
      (handler request)
      (response/redirect (path/root-link request "/login")))))

(defn current-user
  "Method used to get the current user for the request.  Multi interface allows for use in common runnables functions."
  ([& ignore] (sandbar/session-get :user)))

(defn current-org
  "Obtains the organization from the current user.  If out of session returns nil."
  [& ignore]
  (:organization (current-user)))

(defn current-org-id
  "Obtains the organization from the current user.  If out of session returns nil."
  [& ignore]
  (get-in (current-user) [:organization :id]))