(ns org.healthsciencessc.rpms2.consent-admin.ui.login
  (:require [org.healthsciencessc.rpms2.process-engine.path :as path]))

(defn ui-login-form
  "Generates the login form"
  [ctx]
  (let [error (:error ctx)]
    [:div#login-pane.login-pane {} 
      (if error [:div#error.error (:message error)] nil)
      [:form#login-form.login-form {method="POST"}
        [:input#username.username {:type "text"}]
        [:input#password.password {:type "password"}]
        [:input#login.login {:type "submit"}]]]))