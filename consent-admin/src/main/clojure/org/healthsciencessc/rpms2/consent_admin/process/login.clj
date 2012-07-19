(ns org.healthsciencessc.rpms2.consent-admin.process.login
  (:refer-clojure :exclude [root])
  (:require [org.healthsciencessc.rpms2.process-engine.path :as path]
            [org.healthsciencessc.rpms2.consent-admin.ui.layout :as layout]
            [org.healthsciencessc.rpms2.consent-admin.security :as security]
            [org.healthsciencessc.rpms2.consent-admin.services :as services]
            [org.healthsciencessc.rpms2.consent-admin.ui.login :as ui]
            [sandbar.stateful-session :as sess]
            
            [ring.util.response :as rutil]
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))

;; Provide An Overridable Authentication Process
(defprocess authenticate
  "Authenticates a username password combination with the consent services applicaiton."
  [ctx username password]
  (if-let [user (services/authenticate username password)]
    (if-not (= :invalid user)
      (sess/session-put! :user user))))

;; Register The Root Redirection
(defprocess redirect-root
  "Redirects the request to the get-login process."
  [ctx]
  (rutil/redirect (path/root-link ctx "/login")))

(as-method redirect-root endpoint/endpoints "get")

;; Register The Root Login Redirection
(defprocess redirect-login
  "Redirects the request to the get-security-login process."
  [ctx]
  (rutil/redirect (path/root-link ctx "/security/login")))

(as-method redirect-login endpoint/endpoints "get-login")

;; Register The Login View
(defprocess view-login
  "Redirects the request to the get-security-login process."
  [ctx]
  (if (security/is-authenticated?)
    (rutil/redirect (path/root-link ctx "/view/home"))
    (layout/render ctx "Login" (ui/ui-login-form ctx))))

(as-method view-login endpoint/endpoints "get-security-login")

;; Register The Login Process
(defprocess do-login
  "Performs the login "
  [ctx]
  (sess/session-delete-key! :user)
  (authenticate ctx (get-in ctx [:body-params :username])
                    (get-in ctx [:body-params :password]))
  (if (security/is-authenticated?)
    (rutil/redirect (path/root-link ctx "/view/home"))
    (view-login (assoc ctx :error {:message "The username or password provided wasn't correct."}))))

(as-method do-login endpoint/endpoints "post-security-login")

;; Register The Home View
(defprocess view-home
  "Generates a view of the home/landing page."
  [ctx]
  (layout/render ctx "Home"))

(as-method view-home endpoint/endpoints "get-view-home")

;; Register The Logout Process
(defprocess do-logout
  "Redirects the request to the get-security-login process."
  [ctx]
  (sess/session-delete-key! :user)
  (redirect-login ctx))

(as-method do-logout endpoint/endpoints "get-logout")
