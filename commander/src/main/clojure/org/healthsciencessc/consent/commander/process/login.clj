(ns org.healthsciencessc.consent.commander.process.login
  (:refer-clojure :exclude [root])
  (:require [pliant.webpoint.url :as url]
            [org.healthsciencessc.consent.client.core :as services]
            [org.healthsciencessc.consent.client.whoami :as whoami]
            [org.healthsciencessc.consent.commander.ui.layout :as layout]
            [org.healthsciencessc.consent.commander.security :as security]
            [org.healthsciencessc.consent.commander.ui.login :as ui]
            [sandbar.stateful-session :as sess]
            
            [ring.util.response :as rutil]
            [pliant.webpoint.request :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))

;; Provide An Overridable Authentication Process
(defprocess authenticate
  "Authenticates a username password combination with the consent services applicaiton."
  [ctx username password]
  (services/authenticate username password))

;; Register The Root Redirection
(defprocess redirect-root
  "Redirects the request to the get-login process."
  [ctx]
  (rutil/redirect (url/root-link ctx "/login")))

(as-method redirect-root endpoint/endpoints "get")

;; Register The Root Login Redirection
(defprocess redirect-login
  "Redirects the request to the get-security-login process."
  [ctx]
  (rutil/redirect (url/root-link ctx "/security/login")))

(as-method redirect-login endpoint/endpoints "get-login")

;; Register The Login View
(defprocess view-login
  "Redirects the request to the get-security-login process."
  [ctx]
  (if (whoami/identified?)
    (rutil/redirect (url/root-link ctx "/view/home"))
    (layout/render ctx "Login" (ui/ui-login-form ctx))))

(as-method view-login endpoint/endpoints "get-security-login")

;; Register The Login Process
(defprocess do-login
  "Performs the login "
  [ctx]
  (whoami/deidentify!)
  (authenticate ctx (get-in ctx [:body-params :username])
                    (get-in ctx [:body-params :password]))
  (if (whoami/identified?)
    (rutil/redirect (url/root-link ctx "/view/home"))
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
  (sess/destroy-session!)
  (redirect-login ctx))

(as-method do-logout endpoint/endpoints "get-logout")
