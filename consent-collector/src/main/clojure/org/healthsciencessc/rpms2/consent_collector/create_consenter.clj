(ns org.healthsciencessc.rpms2.consent-collector.create-consenter 
  (:require [hiccup.core :as hiccup]
            [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
            [org.healthsciencessc.rpms2.consent-collector.helpers :as helper]
            [hiccup.page-helpers :as hpages]
            [hiccup.form-helpers :as hform])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

(defn view 
  "Returns form to create a consenter."
  [ctx]
  (helper/rpms2-page 
    (helper/standard-form "POST" (helper/mypath "/create/consenter")
     [:p "Present form for the user fill out to Create consenter" ]
       [:div#consenter-details  (for [v [ "firstname"
                                     "middlename"
                                     "lastname"
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
        (helper/submit-button "create-consenter-form"))
	:title (i18n :hdr-create-consenter)))

(defn perform
  "Performs...  "

  [{{:keys [userid password]} :body-params} ]
  ;;dsa/post-security-authenticate
  (debug "perform-not done")
  (helper/myredirect "/view/select/location"))


