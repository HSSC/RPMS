(ns org.healthsciencessc.rpms2.consent-collector.select-unlockcode
  (:require
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only [session-get session-put! session-delete-key! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

(defn- valid-lock-code?
	[lockcode]
  (and (string? lockcode)
       (re-matches #"\d{4}" lockcode)))

(defn perform 
  [ { {:keys [lockcode]} :body-params } ]
  (debug "select_unlockcode/perform lockcode is "  lockcode " expected "
         (session-get :lockcode) )
  (if (or (= lockcode (session-get :lockcode))
          (= nil (session-get :lockcode)))
    (do (session-delete-key! :lockcode )
        (helper/init-review)
        (helper/myredirect  "/review/consents"))
    (helper/flash-and-redirect :flash-invalid-lockcode "/view/unlock")))

(defn view 
  "Displays form for entering lockcode."
  [_]
  (helper/rpms2-page 
    (helper/post-form "/view/unlock" 
        (list [:div.left (i18n :unlock-code-form-enter-lock-code ) ]
              [:input {:id "lockcode" 
                       :name "lockcode" 
                       :type "number" 
                       :required "" 
                       :length 4 
                       :min 0 
                       :max "9999" 
                       :placeholder (i18n :unlock-code-form-enter-lock-code-placeholder) } ])
        (helper/submit-btn { :value (i18n "unlock-code-form-submit-button") :name "unlock-code-form-submit-button" } ))
   :title (i18n :hdr-select-unlockcode)
    )) 
