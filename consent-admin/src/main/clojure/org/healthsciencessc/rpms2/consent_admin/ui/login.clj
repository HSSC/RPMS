(ns org.healthsciencessc.rpms2.consent-admin.ui.login
  (:require [org.healthsciencessc.rpms2.process-engine.path :as path]
            [hiccup.form :as form]))

(defn ui-login-form
  "Generates the login form"
  [ctx]
  (let [error (:error ctx)]
    [:div#login-pane  
        (if error 
          [:div#error (:message error)] nil)
        (form/form-to [:post ""] 
          (form/label "username" "Username")
          (form/text-field "username")
          (form/label "password" "Password")
          (form/password-field "password")
          (form/submit-button {:class "submit"} "Login"))]))
