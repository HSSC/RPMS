(ns org.healthsciencessc.rpms2.consent-collector.select-consenter
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

(defn view 
   "Returns form to search consenter with a button to create the consenter"
  [ctx]
  (helper/rpms2-page 
    (helper/standard-form "POST" (helper/mypath "/view/search/consenters") 
    (for [s [ "firstname" 
	      "lastname" 
	      "consenter-id" 
	      "date-of-birth" 
	      "zipcode" ]]
              (helper/text-field3 "search-consenters-form" s))
       	 (helper/submit-button "search-consenters-form") 
         [:div#tiny [:form {:method "GET" :action (helper/mypath "/view/create/consenter") } 
      		(helper/submit-button "create-consenter-form") ]]
    ) :title (i18n :hdr-search-consenters)))

(defn perform
  "Performs...  "

  [{{:keys [userid password]} :body-params} ]
  ;;dsa/post-security-authenticate
  (debug "perform-not done")
  (helper/myredirect "/view/select/location"))


