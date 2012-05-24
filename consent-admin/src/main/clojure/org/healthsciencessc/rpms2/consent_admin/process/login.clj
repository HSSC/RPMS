(ns org.healthsciencessc.rpms2.consent-admin.process.login
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.process-engine.path :as path]
            [org.healthsciencessc.rpms2.consent-admin.ui.layout :as layout]
            [org.healthsciencessc.rpms2.consent-admin.config :as config]
            [org.healthsciencessc.rpms2.consent-admin.security :as security]
            [hiccup.element :as elem]
            [hiccup.page :as page]
            [org.healthsciencessc.rpms2.consent-admin.services :as services]
            [sandbar.stateful-session :as sess]
            [ring.util.response :as rutil])
  (:use [org.healthsciencessc.rpms2.consent-admin.ui.login]
        [clojure.pprint]
        [sandbar.stateful-session])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(defn handle-root-request
  "Redirects the request to the get-login process."
  [ctx]
  (rutil/redirect (path/root-link ctx "/login")))

(defn redirect-to-security-login
  "Redirects the request to the get-security-login process."
  [ctx]
  (rutil/redirect (path/root-link ctx "/security/login")))

(defn authenticate
  "Authenticates a username password combination with the consent services applicaiton."
  [ctx username password]
  (if-let [user (services/authenticate username password)]
    (if-not (= :invalid user)
      (sess/session-put! :user user))))

(defn generate-login-page
  ""
  [ctx]
  (if (security/is-authenticated?)
    (rutil/redirect (path/root-link ctx "/view/home"))
    (layout/render ctx "Login" (ui-login-form ctx))))

(defn do-login
  ""
  [ctx]
  (sess/session-delete-key! :user)
  (authenticate ctx (get-in ctx [:body-params :username])
                    (get-in ctx [:body-params :password]))
  (if (security/is-authenticated?)
    (rutil/redirect (path/root-link ctx "/view/home"))
    (generate-login-page (assoc ctx :error {:message "The username or password provided wasn't correct."}))))

(defn logout
  [params]
  (sess/session-delete-key! :user)
  redirect-to-security-login)

(def process-defns
  [
   ;; Handles a root request. Redirects to /login
   {:name "get"
    :runnable-fn (constantly true)
    :run-fn handle-root-request}

   ;; Redirects to /security/login
   {:name "get-login"
    :runnable-fn (constantly true)
    :run-fn redirect-to-security-login}

   {:name "get-logout"
    :runnable-fn (constantly true)
    :run-fn logout}

   ;; Generates the login page
   {:name "get-security-login"
    :runnable-fn (constantly true)
    :run-fn generate-login-page}

   ;; Provides the landing page for an authenticated user.
   {:name "get-view-home"
    :runnable-fn (constantly true)
    :run-fn (fn [ctx] (layout/render ctx "Home"))}

   ;; Performs the authentication.
   {:name "post-security-login"
    :runnable-fn (constantly true)
    :run-fn do-login}
   ])

(process/register-processes (map #(DefaultProcess/create %) process-defns))

