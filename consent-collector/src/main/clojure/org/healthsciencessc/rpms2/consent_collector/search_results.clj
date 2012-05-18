(ns org.healthsciencessc.rpms2.consent-collector.search-results
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only [session-get session-put! session-delete-key! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))


(defn- search-selected 
  "If yes button was selected, go to select protocols page.
  Otherwise, go back to the search page."
  [parms]

  (debug "search-selected " parms)
  (let [ y  (:search-consenter-results-yes parms)
	patient-id (:patient-id parms ) 
	patient-name (:patient-name parms ) 
	patient-encounter-date (:patient-encounter-date parms ) ]
	(if (not (empty? y)) 
            (if (or (empty? patient-name)
                    (= patient-name "no patient")) 
                (do 
                   (flash-put! :header (i18n :flash-search-consenter-results-no-selection ) )
	   	   (helper/myredirect "/view/select/consenter"))
	        (do 
		   (session-put! :patient-id patient-id)
		   (session-put! :patient-name patient-name)
		   (session-put! :patient-encounter-date patient-encounter-date)
    		   (helper/myredirect "/view/select/protocols")))
	   (do
	    	(session-delete-key! :patient-id)
	   	(helper/myredirect "/view/select/consenter")))))
    

(defn view 
  [ctx]
  (search-selected (:query-params ctx)))

(defn perform 
  [ctx]
  (search-selected (:body-params ctx)))

   
