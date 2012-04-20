(ns org.healthsciencessc.rpms2.consent-collector.search-consenter
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))


(defn consenter-details
  "This is right hand side of the search results page, showing
  the details associated with the selected consenter (e.g. patient)"
  []
  ;; (let [i18n (partial i18n "search-consenter-results")]
  [:div#consenter-details
    [:h1 (i18n "consenter-details-verify-record-details" )]
    [:ul (for [v [ "visit-number" 
                   "medical-record-number" 
                   "encounter-date" ]]
                   [:li {:data-role "fieldcontain" } 
                   [:label {:for v :class "labeldim" } (i18n "search-consenter-results-form" v "label") ]
                   [:div.highlightvalue { :id (str "consenter-" v ) } ]])]

    [:h1 (i18n "consenter-details-verify-patient-details" )]
    [:ul (for [v [ "name"
                 "date-of-birth"
                 "last-4-digits-ssn"
                 "zipcode"
                 "referring-doctor"
                 "primary-care-physician"
                 "primary-care-physician-city" ]]
          [:li {:data-role "fieldcontain" } 
            [:label {:for v :class "labeldim" } (i18n "search-consenter-results-form" v "label") ]
            [:div.highlightvalue { :id (str "consenter-" v ) } ]]) ]

	;; section for "Is this correct?" with Yes/No buttons
   [:div.areaTitleCentered (i18n "consenter-details-confirmation-question" )]

   ;; save current selection 
   [:form#other-section { :method "GET" :action (helper/mypath "/view/search/results") } 
      [:input {:type "hidden" :name "patient-id" :id "patient-id" :value "no patient" } ]
      [:input {:type "hidden" :name "patient-name" :id "patient-name" :value "no patient" } ]
      [:input {:type "hidden" :name "patient-encounter-data" :id "patient-name" :value "no patient" } ]
      (helper/submit-button "search-consenter-results-yes") 
      (helper/submit-button "search-consenter-results-no")]])


(defn generate-consenter-results-list
  "Emit clickable list of consenters on the Search Results page."
  [results]

  [:ul.search-results
  (for [{mrn :medical-record-number fn :firstname ln :lastname :as user} results]
    [:li {:onclick "org.healthsciencessc.rpms2.core.consenter_search_result_clicked(this)"
           :data-user (pr-str user)} fn " " ln 
    [:div.secondary "MRN: " mrn ] ])])

(defn page-search-consenter-results
  [results]
  (try
    (helper/rpms2-page-two-column
      [:div#search-consenter-list (generate-consenter-results-list results) ]
      (consenter-details) 
      :title (i18n :hdr-search-consenter-results) ) 
  (catch Exception ex  
    (error "FAILED page-search-consenter-results " (.printStackTrace ex)) 
    (helper/rpms2-page (str "x. failed "  ex) :title (str "x. failed " (.getMessage ex))))))


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
      (page-search-consenter-results results))))

(defn get-view
  [ctx]
  ;(debug "default-get-view-search-consenters" ctx)
  (process-search-consenters (:query-params ctx)))

(defn post-view
  [ctx]
  ;(debug "default-post-view-search-consenters " ctx)
  (process-search-consenters (:body-params ctx)))

