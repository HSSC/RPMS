(ns org.healthsciencessc.rpms2.consent-collector.create-consenter
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

(defn view 
  "Returns form to create a consenter."
  [ctx]
  (helper/rpms2-page 
    (helper/standard-form "POST" (helper/mypath "/view/create/consenter")
     [:p "Present form for the user fill out to Create consenter" ]
       [:div#consenter-details  (for [v [ "first-name"
                                     "middle-name"
                                     "last-name"
                                     "local-identifier"
                                     "local-identifier-type"
                                     "gender"
                                     "race"
                                     "religion"
                                     "address"
                                     "phone"
                                     "email"
                                     "date-of-birth"
                                     ]]
	(helper/text-field3 "create-consenter-form" v))]

        [:div.centered
          (helper/submit-button "create-consenter-form" 
                   (i18n "create-consenter-form-submit-button" ) 
                   "create-consenter") ])
	:title (i18n :hdr-create-consenter)))

(defn perform
  "Performs...  "

  [{{:keys [userid password]} :body-params} ]
  (debug "create consenter not implemented")
  (helper/myredirect "/view/select/location"))


