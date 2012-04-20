(ns org.healthsciencessc.rpms2.consent-collector.login
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! destroy-session! ]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

(defn logout
  "Clears session data and sends user back to login page."  
  [ctx]
  (flash-put! :header (str "Logging out") )

  (destroy-session!)
  (helper/remove-session-data)
  (helper/myredirect "/view/login"))

(defn default-get-login
  "Redirect to view-login"
  [ctx]
  (helper/myredirect "/view/login"))

(defn view 
  "Returns login form"
  [ctx]

  (helper/rpms2-page 
     [:div.standardForm
     [:form#loginForm {:action (helper/mypath "/view/login") 
                       :method "POST" :name "loginForm" :id "loginForm" 
                       :data-ajax "false" } 
      (i18n :login-form-username) 
      [:input {:id "username" :name "userid" :type "text" :required "" 
               :placeholder (i18n "login-form-username-placeholder") } ]

      (i18n :login-form-password) 
      [:input {:name "password" :type "password" :required ""
               :placeholder (i18n "login-form-password-placeholder") }] 

      (helper/submit-button  "login-form") ]] :title (i18n :hdr-login)))

(defn perform
  "Authenticates using username and password.  
   If successful, saves user in session and redirects to /view/select/location; 
   otherwise sets flash error message and redirects to /view/login.  "

  [{{:keys [userid password]} :body-params} ]
  (let [resp (dsa/authenticate userid password)]
    (if (= (:status resp) 500)
        (do
           (flash-put! :header 
                (str "Unable to process request " (:error-message resp)) )
           (helper/myredirect "/view/login" ))
    (if (= (:status resp) 200)
      (do
	(let [bb (:json resp) ]
                (println "USER JSON IS " bb)
	 	(helper/remove-session-data)
        	(debug "USER RECORD => " bb )
        	(session-put! :user bb)
	)
        (debug "LOGIN succeeded: " userid)
        (helper/myredirect "/view/select/location"))
      (do 
 	(helper/remove-session-data)
        (flash-put! :header (i18n :flash-invalid-login))
        (debug "LOGIN FAILED:" userid)
        (helper/myredirect "/view/login"))))))


(defn default-get-view-not-authorized 
  "Display when user has no authorized locations."
  [_]
  (helper/rpms2-page 
    [:div.warningbox (i18n :not-authorized-message) ]
                      :title (i18n :hdr-not-authorized )))

