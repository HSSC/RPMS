;; Provides the configuration of the admin
(ns org.healthsciencessc.rpms2.consent-admin.process.login
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.process-engine.path :as path]
            [org.healthsciencessc.rpms2.consent-admin.ui.landscape :as landscape]
            [org.healthsciencessc.rpms2.consent-admin.config :as config]
            [org.healthsciencessc.rpms2.consent-admin.security :as security]
            [ring.util.response :as rutil]
            [hiccup.page :as page])
  (:use [org.healthsciencessc.rpms2.consent-admin.ui.login]
        [clojure.pprint]
        [sandbar.stateful-session])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(defn redirect-to-security-login
  "Redirects the request to the get-security-login process."
  [ctx]
   (rutil/redirect (path/root-link ctx "/security/login")))

(defn generate-login-page
  ""
  [ctx]
  (page/html5 
    (landscape/head ctx {}) 
    (landscape/body-no-session ctx {:content (ui-login-form ctx)})))

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
   ;; Redirects to /security/login
   {:name "get-login"
    :runnable-fn (constantly true)
    :run-fn redirect-to-security-login}

   ;; Generates the login page
   {:name "get-security-login"
    :runnable-fn (constantly true)
    :run-fn generate-login-page}

   {:name "get-view-home"
    :runnable-fn (constantly true)
    :run-fn (fn [_] "You've successfully authenticated.")}
   
   {:name "post-security-login"
    :runnable-fn (constantly true)
    :run-fn do-login}

   {:name "get-message"
    :runnable-fn (constantly true)
    :run-fn (fn [ctx]
              (str "Normal: " (config/config "message") " Keyword: " (:message config/config)))}

   ])

(process/register-processes (map #(DefaultProcess/create %) process-defns))
