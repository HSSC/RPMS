(ns org.healthsciencessc.rpms2.consent-collector.select-lockcode
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only [session-get session-put! session-delete-key! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

(defn- valid-lock-code?
	[lockcode]
  (and (string? lockcode)
       (re-matches #"\d{4}" lockcode)))

(defn perform 
  "Validates lock code.  If valid, continue on to /view/select/consenter.
  Otherwise, go back to /view/select/lock-code to try again.
  Sets flash message if lock code is invalid."

  [ { {:keys [lockcode]} :body-params } ]
  (debug "select_lockcode/perform lockcode is "  lockcode)
  (if (valid-lock-code? lockcode) 
    (do
      (session-put! :lockcode lockcode)
      (helper/myredirect "/view/select/consenter"))
    (do
      (session-delete-key! :lockcode)
      (info "Invalid lockcode " lockcode)
      (flash-put! :header (i18n :flash-invalid-lockcode))
      (helper/myredirect "/view/select/lock-code"))))

(defn view 
  "Generates form for entering lockcode - a required
   4 digit number."
  [_]
  (helper/rpms2-page 
    (helper/standard-form "POST" (helper/mypath "/view/select/lock-code" )
      (i18n :lock-code-form-enter-lock-code ) 
      [:input {:name "lockcode" 
               :type "number" 
               :required "" 
               :length 4 
               :min 0 
               :max "9999"}]

      (let [form-name "lock-code-form"]
        [:div.centered [:input {:type "submit" 
               :data-theme "a" 
               :data-role "button" 
               :data-inline "true" 
               :value (i18n "lock-code-form-submit-button") 
               :name "lock-code-form-submit-button" } ]]))
     :title (i18n :hdr-select-lockcode))) 
