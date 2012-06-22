(ns org.healthsciencessc.rpms2.consent-collector.search-results
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper]
   [org.healthsciencessc.rpms2.consent-domain.types :as types]
   [org.healthsciencessc.rpms2.consent-domain.core :as domain])
  (:use [sandbar.stateful-session :only [session-get session-put! session-delete-key! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

(def consenter-keys (disj (into #{} (-> domain/default-data-defs
                      (get types/consenter)
                      :attributes
                      :keys)) :id :active))   ;; change this when domain/core has a helper fn

(defn- search-selected 
  "If yes button was selected, go to select protocols page.
  Otherwise, go back to the search page."
  [parms]

  (debug "search-selected " parms)
  (let [y (:search-consenter-results-yes parms)
	consenter-id (:consenter-id parms)]
    (debug "Consenter-id Selected WAS: " parms)
    ;; Re-add encounter date from parms
	(if (not (empty? y)) 
            (if consenter-id
	        (do 
                   (helper/set-consenter (first (:json (dsa/dsa-search-consenters {:consenter-id consenter-id} nil))))
                   (session-delete-key! :search-results)
                   (session-delete-key! :search-params)
    		   (helper/myredirect "/view/select/encounter"))
                (do 
                   (flash-put! :header (i18n :flash-search-consenter-results-no-selection ) )
	   	   (helper/myredirect "/view/select/consenter")))
	   (do
	    	(session-delete-key! :consenter)
	   	(helper/myredirect "/view/select/consenter")))))

(defn view 
  [ctx]
  (search-selected (:query-params ctx)))

(defn perform 
  [ctx]
  (search-selected (:body-params ctx)))

   
