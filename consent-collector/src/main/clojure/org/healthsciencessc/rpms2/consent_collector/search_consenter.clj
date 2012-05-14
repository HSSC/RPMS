(ns org.healthsciencessc.rpms2.consent-collector.search-consenter
  (:require
   [org.healthsciencessc.rpms2.consent-collector [ dsa-client :as dsa]
    [helpers :as helper]
    [select-consenter :as sc]]
   [clojure.string :as s])
  (:use [org.healthsciencessc.rpms2.consent-collector.debug :only [debug!]])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info warn error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

(defn consenter-details
  "This is right hand side of search results page, showing
  details associated with selected consenter (e.g. patient)"
  []

  [:div
  ;;(let [scrf-i18n (partial i18n :search-consenter-results-form)
  ;; Instead of 'first-name' and 'last-name' just display name
  ;;     displayed-fields [:name :consenter-id :date-of-birth :zipcode ] ]

    [:div#consenter-details
      [:h1 (i18n :search-consenter-results-form :selected-consenter) ]
      [:ul (doall (for [vn [:name :consenter-id :date-of-birth :zipcode :date-of-birth ] ] 
             [:li {:data-role "fieldcontain" } 
             [:label {:for (name vn) :class "labeldim" } 
                (i18n :search-consenter-results-form (name vn) :label) ]
             [:div.highlightvalue { :id (str "consenter-" (name vn) ) } "..." ]] ) ) ]]

     ;; section for "Is this correct?" with Yes/No buttons
     [:h2.centered (i18n :search-consenter-results-form :confirmation-question )]

     [:form#other-section { :method "POST" :action (helper/mypath "/view/search/results") } 

     ;; save current selection 
     ;; obviously we should no longer save these beause these aren't the fields
      (for [f ["patient-id" "patient-name" "patient-encounter-data" ]]
            [:input {:type "hidden" :name f :id f :value "no patient"}])

      [:div.centered
      (helper/submit-button "NO" "search-consenter-results-no")
      (helper/submit-button "YES" "search-consenter-results-yes") 
       ]] ])

(defn handle-search-results
  "Search results are available in the session.
  This will display a two column page."

  [parms]

  (let [results (session-get :search-results)]
   (if (empty? results)
       (helper/flash-and-redirect :flash-no-consenters-match-search "/view/select/consenter")
       (helper/rpms2-page-two-column
             [:div#search-consenter-list
             [:ul.search-results
                 (for [{ zipcode :zipcode, fname :first-name, ln :last-name, :as user} results]
                      [:li {:onclick "org.healthsciencessc.rpms2.core.consenter_search_result_clicked(this)"
                            :data-user (pr-str user)} fname " " ln 
                      [:div.secondary "Zipcode " zipcode ] ])]]
             [:div#consenter-details (consenter-details) ]
             (i18n :hdr-search-consenter-results)))))

(defn view
  [ctx]
  (handle-search-results (:query-params ctx)))

(defn perform
  [ctx]
  (handle-search-results (:body-params ctx)))

;(debug! handle-search-results)
;(debug! consenter-details)
