;; Provides the configuration of the admin
(ns org.healthsciencessc.rpms2.consent-admin.process.login
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.process-engine.path :as path]
            [org.healthsciencessc.rpms2.consent-admin.ui.landscape :as landscape]
            [org.healthsciencessc.rpms2.consent-admin.config :as config]
            [ring.util.response :as rutil]
            [hiccup.page :as page])
  (:use [org.healthsciencessc.rpms2.consent-admin.ui.login])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))


(defn redirect-to-security-login
  "Redirects the request to the get-security-login process."
  [ctx]
   (rutil/redirect (path/root-link ctx "/security/login"))
  )

(defn generate-login-page
  ""
  [ctx]
  (page/html5 
    (landscape/head ctx {}) 
    (landscape/body-no-session ctx {:content (ui-login-form ctx) })))

(defn authenticate
  "Authenticates a username password combination with the consent services applicaiton."
  [ctx username password]
  (if (and (= "admin" username) (= "password" password))
    {
     :id 10
     :title "Mr."
     :first-name "Bob"
     :middle-name ""
     :last-name "Smith"
     :suffix "Jr."
     :username username
     :password password
     :organization {:id 2 :name "Med Univ" :code  "musc"}
     :role-mappings [
            {
              :role {:id 4 :name "Consent Collector" :code "cc"}
              :organization {:id 2 :name "Med Univ" :code "musc"}
              :location {:id 2 :name "Registration" :code "reg"}
            }
        ]
     }
    nil))

(defn do-login
  ""
  [ctx]
  (println "HERE MOFO")
  (let [username (ctx "username")
        password (ctx "password")
        user (authenticate ctx username password)]
    (if user
      (rutil/redirect (path/root-link ctx "/view/home"))
      (generate-login-page (assoc ctx :error {:message "The username/password provided"})))))

(def process-defns
  [
   ;; Redirects to /security/login
   {:name "get-login"
    :runnable-fn (fn [ctx] true)
    :run-fn redirect-to-security-login}

   ;; Generates the login page
   {:name "get-security-login"
    :runnable-fn (fn [ctx] true)
    :run-fn generate-login-page}

   ;; curl -i -X GET -H "Content-type: application/json" http://localhost:3000/organization?organization=<ID>
   {:name "post-security-login"
    :runnable-fn (fn [ctx] true)
    :run-fn do-login}

   ;; curl -i -X GET -H "Content-type: application/json" http://localhost:3000/organization?organization=<ID>
   {:name "get-message"
    :runnable-fn (fn [ctx] true)
    :run-fn (fn [ctx]
              (str "Normal: " (config/config "message") " Keyword: " (:message config/config)))}

   ])

(process/register-processes (map #(DefaultProcess/create %) process-defns))
