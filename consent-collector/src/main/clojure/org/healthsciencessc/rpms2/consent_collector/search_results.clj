(ns org.healthsciencessc.rpms2.consent-collector.search-results
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only [session-get session-put! session-delete-key! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))


(defn handle-search-selection-response
  "If yes button was selected, go to the select protocols page.
  Otherwise, go back to the search page.

  TODO: Make sure that a patient id was actually selected."
  [ctx]
  (let [ y (:search-consenter-results-yes-submit-button (:query-params ctx))
	patient-id (:patient-id (:query-params ctx)) 
	patient-name (:patient-name (:query-params ctx)) 
	patient-encounter-date (:patient-encounter-date (:query-params ctx)) 
	]
	(if (= y "Yes") 
	   (do 
		(session-put! :patient-id patient-id)
		(session-put! :patient-name patient-name)
		(session-put! :patient-encounter-date patient-encounter-date)
    		(helper/myredirect "/view/select/protocols"))
	   (do
	    	(session-delete-key! :patient-id)
	   	(helper/myredirect "/view/select/consenter")))))

