;; Provides the configuration of the admin
(ns org.healthsciencessc.rpms2.consent-admin.process.login
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.process-engine.path :as path]
            [org.healthsciencessc.rpms2.consent-admin.ui.layout :as layout]
            [org.healthsciencessc.rpms2.consent-admin.config :as config]
            [org.healthsciencessc.rpms2.consent-admin.security :as security]
            [ring.util.response :as rutil]
            [hiccup.element :as elem]
            [hiccup.page :as page])
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
  (when (and (= "admin" username) (= "password" password))
    (session-put! :user
      {:id 10
      :title "Mr."
      :first-name "Bob"
      :middle-name ""
      :last-name "Smith"
      :suffix "Jr."
      :username username
      :password password
      :organization {:id 2 :name "Med Univ" :code  "musc"}
      :role-mappings [{:role {:id 4 :name "Consent Collector" :code "cc"}
                      :organization {:id 2 :name "Med Univ" :code "musc"}
                      :location {:id 2 :name "Registration" :code "reg"} }]})))



(defn generate-login-page
  ""
  [ctx]
  (if (security/is-authenticated?)
    (rutil/redirect (path/root-link ctx "/view/home"))
    (layout/layout-no-session ctx (ui-login-form ctx))))


(defn do-login
  ""
  [ctx]
  (authenticate ctx (get-in ctx [:body-params :username])
                    (get-in ctx [:body-params :password]))
    (if (security/is-authenticated?)
      (rutil/redirect (path/root-link ctx "/view/home"))
      (generate-login-page (assoc ctx :error {:message "The username or password provided wasn't correct."}))))

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

   ;; Generates the login page
   {:name "get-security-login"
    :runnable-fn (constantly true)
    :run-fn generate-login-page}

   ;; Provides the landing page for an authenticated user.
   {:name "get-view-home"
    :runnable-fn (constantly true)
    :run-fn (fn [ctx] (layout/layout ctx ""))}

   ;; Performs the authentication.
   {:name "post-security-login"
    :runnable-fn (constantly true)
    :run-fn do-login}
   ])

(process/register-processes (map #(DefaultProcess/create %) process-defns))
