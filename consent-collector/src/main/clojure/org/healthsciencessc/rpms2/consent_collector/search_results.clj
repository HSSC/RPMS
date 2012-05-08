(ns org.healthsciencessc.rpms2.consent-collector.search-results
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only [session-get session-put! session-delete-key! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))


(defn- search-selected 
  "If yes button was selected, go to the select protocols page.
  Otherwise, go back to the search page.

  TODO: Make sure that a patient id was actually selected."
  [parms]

  (debug "search-selected " parms)
  (try
  (let [ y  (:search-consenter-results-yes parms)
        _ (debug "search-selected: y =  " y )
	patient-id (:patient-id parms ) 
        _ (debug "patient id " patient-id )
	patient-name (:patient-name parms ) 
        _ (debug "patient name  " patient-name )
	patient-encounter-date (:patient-encounter-date parms ) 
	]
	(if (not (empty? y)) 
            (if (or (empty? patient-name)
                    (= patient-name "no patient")) 
                (do 
                   (flash-put! :header "No patient selected." )
	   	   (helper/myredirect "/view/select/consenter"))
	        (do 
		   (session-put! :patient-id patient-id)
		   (session-put! :patient-name patient-name)
		   (session-put! :patient-encounter-date patient-encounter-date)
                   (flash-put! :header (print-str "Patient selected: "  patient-name))
    		   (helper/myredirect "/view/select/protocols")))
	   (do
	    	(session-delete-key! :patient-id)
                (flash-put! :header "Search again" )
	   	(helper/myredirect "/view/select/consenter"))))
    
    (catch Exception ex (debug "Exception in search-selected " ex))))

(defn view 
  [ctx]
  (search-selected (:query-params ctx)))

(defn perform 
  [ctx]
  (search-selected (:body-params ctx)))

   
