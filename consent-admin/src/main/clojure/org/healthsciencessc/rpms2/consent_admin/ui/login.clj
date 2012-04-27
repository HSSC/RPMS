(ns org.healthsciencessc.rpms2.consent-admin.ui.login
  (:require [org.healthsciencessc.rpms2.process-engine.path :as path]))

(defn ui-login-form
  "Generates the login form"
  [ctx]
  (let [error (:error ctx)]
    [:div#login-pane {} 
      (if error [:div#error (:message error)] nil)
      [:form#login-form {:method "POST"}
        [:input#username {:type "text" :name "username"}]
        [:input#password {:type "password" :name "password"}]
        [:input#login {:type "submit"}]]]))
