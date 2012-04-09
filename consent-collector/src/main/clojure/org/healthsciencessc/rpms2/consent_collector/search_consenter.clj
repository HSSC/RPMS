(ns org.healthsciencessc.rpms2.consent-collector.search-consenter 
  (:require [hiccup.core :as hiccup]
            [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
            [org.healthsciencessc.rpms2.consent-collector.helpers :as helper]
            [hiccup.page-helpers :as hpages]
            [hiccup.form-helpers :as hform])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

(defn consenter-details
  "This is right hand side of the search results page, showing
  the details associated with the selected consenter (e.g. patient)"
  []
  [:div
  [:div#consenter-details-section  
   [:div.areaTitle (i18n "consenter-details-verify-record-details" )]
   [:div.bordered-half 
     (for [v [ "visit-number" "medical-record-number" "encounter-date" ]]
	(helper/name-value-bold-input "search-consenter-results-form" v (str "consenter-" v))) 
  ]]

  [:div.areaTitle (i18n "consenter-details-verify-patient-details" )]
  [:div.bordered-half 
    [:div#consenter-details  
       (for [v [ "name"
                 "date-of-birth"
                 "last-4-digits-ssn"
                 "zipcode"
                 "referring-doctor"
                 "primary-care-physician"
                 "primary-care-physician-city" ]]
           (helper/name-value-bold "search-consenter-results-form" v (str "consenter-" v)))]]

	;; section for "Is this correct?" with the selected patient-id
	;; and Yes/No buttons
        [:div.areaTitleCentered (i18n "consenter-details-confirmation-question" )]

  	[:form#other-section { :method "GET" :action (helper/mypath "/view/search/results") } 
   	  [:input {:type "hidden" :name "patient-id" :id "patient-id" :value "no patient" } ]
   	  [:input {:type "hidden" :name "patient-name" :id "patient-name" :value "no patient" } ]
   	  [:input {:type "hidden" :name "patient-encounter-data" :id "patient-name" :value "no patient" } ]
          (helper/submit-button "search-consenter-results-yes") 
          (helper/submit-button "search-consenter-results-no") 
	]
	])


(defn generate-consenter-results-list
 "Generate the clickable list of consenters on the 
  Search Results page (left side)."
 [results]

 (for [{first :firstname last :lastname :as user} results]
    [:div {:onclick "org.healthsciencessc.rpms2.core.consenter_search_result_clicked(this)"
           :data-user (pr-str user)}
          [:span.standout (str first " " last ) ]
    [:div.secondary (str "MRN: " (:medical-record-number user) ) ] ]))

(defn page-search-consenter-results
   [results]
   (try
	(helper/rpms2-page-two-column
		      (generate-consenter-results-list results) 
       		      (consenter-details) 
		      :title (i18n :hdr-search-consenter-results) ) 
   (catch Exception ex 
        (do 
	(error "FAILED page-search-consenter-results " (.printStackTrace ex)) 
        (helper/rpms2-page (str "x. failed "  ex) :title (str "x. failed " (.getMessage ex))))
   )))


(defn process-search-consenters
  ;; TODO: add the org/loc from the session into the
  ;; query params; remove the submit button
  ;; maybe use a filter to select the correct parameters
  ;; and add in user defaults
   [parms]
   (let [results (dsa/search-consenters parms)]
      (if (empty? results) 
	(do 
        	(flash-put! :header (i18n :flash-no-consenters-match-search))
        	(helper/myredirect "/view/select/consenter"))
         (do 
    		(page-search-consenter-results results)))))


(defn get-view
  [ctx]
  (debug "default-get-view-search-consenters" ctx)
  (process-search-consenters (:query-params ctx)))

(defn post-view
  [ctx]
  (debug "default-post-view-search-consenters " ctx)
  (process-search-consenters (:body-params ctx)))

#_(defn view 
  "Returns view "
  [ctx]
  (helper/rpms2-page "SEARCH CONSENTER" :title (i18n :hdr-login)))

#_(defn perform
  "Performs...  "

  [{{:keys [userid password]} :body-params} ]
  ;;dsa/post-security-authenticate
  (debug "perform-not done")
  (helper/myredirect "/view/select/location"))


