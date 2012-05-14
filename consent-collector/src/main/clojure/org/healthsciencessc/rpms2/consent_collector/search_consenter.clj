(ns org.healthsciencessc.rpms2.consent-collector.search-consenter
  (:require
   [org.healthsciencessc.rpms2.consent-collector [ dsa-client :as dsa]
    [helpers :as helper]
    [select-consenter :as sc]]
   [clojure.string :as s])
  (:use [org.healthsciencessc.rpms2.consent-collector.debug :only [debug!]])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info warn error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n i18n-label-for]]))

(def search-results-verify-fields [:name :consenter-id :zipcode :dob ])

(defn- display-search-items-column 
  [results]

  [:div#search-consenter-list
     [:ul.search-results
         (for [{ zip :zipcode, 
                fn :first-name, 
                ln :last-name, 
                consenter-id  :consenter-id, 
                dob :dob  
                :as user} results]
            [:li {:onclick "org.healthsciencessc.rpms2.core.consenter_search_result_clicked(this)"
                  :data-user (pr-str user)} fn " " ln 
             (println "USER IS [" (pr-str user) "]")
             (debug "ZZZ USER IS [" (pr-str user) "]")
             (debug "ZZZ zip [" zip  "]")
             (debug "ZZZ consenter-id [" consenter-id  "]")
             (debug "ZZZ dob [" dob "]")
                 [:div.secondary "Zipcode " zip ] ])]])

(defn- verify-section 
  "This is right hand side of search results page, showing
  details associated with selected consenter (e.g. patient)"
  []

  (list 
    [:div#consenter-details-section
    [:div#consenter-details 
     [:h1 (i18n :search-consenter-results-form :selected-consenter) ]
     [:ul (for [vn search-results-verify-fields ] 
             (list 
               ;(helper/emit-field-def (dsa/consenter-field-defs vn) :search-consenters-results-form (name vn) "readonly")

               [:li {:data-role "fieldcontain" } 
                     [:label {:for (name vn) :class "labeldim" } 
                     (i18n-label-for :search-consenter-results-form (name vn) ) ]
                     [:div.highlightvalue { :id (str "consenter-" (name vn) ) } "..." ]] )) ]]

     ;; section for "Is this correct?" with Yes/No buttons
     [:div.confirm-selection (i18n :search-consenter-results-form :confirmation-question )]

     [:form#other-section { :method "POST" :action (helper/mypath "/view/search/results") } 

     ;; save current selection 
     ;; obviously we should no longer save these because these aren't the fields
      (for [f ["patient-id" "patient-name" "patient-encounter-data" ]]
            [:input {:type "hidden" :name f :id f :value "no patient"}])

      [:div.centered
      (helper/submit-button "NO" "search-consenter-results-no")
      (helper/submit-button "YES" "search-consenter-results-yes") ]]] ))

(defn- results-view
  "Search results are in session (:search-results).
  If there are no search results, go back to the 
  select consenter page.
  Else display Search results as two column page."

  [parms]

  (let [results (session-get :search-results)]
   (if (empty? results)
       (helper/flash-and-redirect :flash-no-consenters-match-search "/view/select/consenter")
       (helper/rpms2-page-two-column
         (display-search-items-column results)
         (verify-section)
         (i18n :hdr-search-consenter-results)))))

(defn view
  [ctx]
  (results-view (:query-params ctx)))

(defn perform
  [ctx]
  (results-view (:body-params ctx)))

;(debug! handle-search-results)
;(debug! consenter-details)
