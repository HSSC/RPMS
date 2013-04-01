(ns org.healthsciencessc.consent.collector.process.login
  (:refer-clojure :exclude [root])
  (:require [org.healthsciencessc.consent.collector.respond :as respond]
            [org.healthsciencessc.consent.collector.state :as state]
            [org.healthsciencessc.consent.collector.text :as text]
            [org.healthsciencessc.consent.collector.process.authorize :as auth]
            [org.healthsciencessc.consent.collector.ui.action :as action]
            [org.healthsciencessc.consent.collector.ui.form :as form]
            [org.healthsciencessc.consent.collector.ui.layout :as layout]
            [org.healthsciencessc.consent.client.core :as services]
            [pliant.webpoint.request :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))


(def fields [{:name :username :type :username :label (text/text :login.username.label) :contain true :autofocus true}
             {:name :password :type :password :label (text/text :login.password.label) :contain true}])

(def options {:method :post
              :url "/security/login"})

;; Register The Root Redirection
(defprocess redirect-root
  "Redirects the request to the get-login process."
  [ctx]
  (respond/redirect  ctx "/login"))

(as-method redirect-root endpoint/endpoints "get")

;; Register The Root Login Redirection
(defprocess redirect-login
  "Redirects the request to the get-security-login process."
  [ctx]
  (respond/redirect ctx "/security/login"))

(as-method redirect-login endpoint/endpoints "get-login")

;; Register The Login View
(defprocess view-security-login
  "Redirects the request to the get-view-location."
  [ctx]
  (if (auth/is-authenticated?)
    (respond/redirect ctx "/view/select/location")
    (layout/render-page ctx {:title (text/text :login.title) :pageid "Login"} 
                   (form/dataform options 
                                  (form/render-fields {} fields (dissoc (:body-params ctx) :password))
                                  (action/form-submit {:label (text/text :login.submit.label)})))))

(as-method view-security-login endpoint/endpoints "get-security-login")

;; Register The Login Process
(defprocess api-security-login
  "Performs the login "
  [ctx]
  (let [username (get-in ctx [:body-params :username])
        password (get-in ctx [:body-params :password])
        user (auth/authenticate username password)]
    (cond
      (= :invalid-user user)
        (respond/with-error (text/text :login.message.noauth))
      (= :invalid-role user)
        (respond/with-error (text/text :login.message.norole))
      :else
        (respond/reset-view "/view/select/location"))))

(as-method api-security-login endpoint/endpoints "post-security-login")


;; Register The Logout Process
(defprocess api-logout
  "Kills the session and redirects to the root."
  [ctx]
  (state/reset)
  (respond/with-actions {}  "toRoot"))

(as-method api-logout endpoint/endpoints "get-logout")
