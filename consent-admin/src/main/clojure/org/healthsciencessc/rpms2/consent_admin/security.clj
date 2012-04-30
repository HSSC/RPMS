(ns org.healthsciencessc.rpms2.consent-admin.security
  (:require [org.healthsciencessc.rpms2.consent-admin.security :as security]
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

