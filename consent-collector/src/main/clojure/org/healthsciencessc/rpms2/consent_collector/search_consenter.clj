(ns org.healthsciencessc.rpms2.consent-collector.search-consenter
  (:require
   [org.healthsciencessc.rpms2.consent-collector [ dsa-client :as dsa]
    [helpers :as helper]
    [select-consenter :as sc]
    [factories :as factory]]
   [clojure.string :as s])
  (:use [org.healthsciencessc.rpms2.consent-collector.debug :only [debug!]])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info warn error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

(defn consenter-details
  "This is right hand side of search results page, showing
  details associated with selected consenter (e.g. patient)"
  []
  (let [scrf-i18n (partial i18n :search-consenter-results-form)
        ;; Instead of 'first-name' and 'last-name' just display name
        displayed-fields (remove #(or(= % "first-name")(= % "last-name")) 
                (cons "name" dsa/consenter-fields)) ]
    [:div#consenter-details
      [:h1 (scrf-i18n :selected-consenter) ]
      [:ul (for [v displayed-fields]
             [:li {:data-role "fieldcontain" } 
             [:label {:for v :class "labeldim" } (scrf-i18n v :label) ]
             [:div.highlightvalue { :id (str "consenter-" v ) } "..." ]]) ]

     ;; section for "Is this correct?" with Yes/No buttons
     [:h1.centered (scrf-i18n :confirmation-question )]

     [:form#other-section { :method "GET" :action (helper/mypath "/view/search/results") } 

     ;; save current selection 
     ;; obviously we should no longer save these beause these aren't the fields
      (for [f ["patient-id" "name" "patient-name" "patient-encounter-data" ]]
            [:input {:type "hidden" :name f :id f :value "no patient"}])

      [:div.centered
      (helper/submit-button "search-consenter-results-yes") 
      (helper/submit-button "search-consenter-results-no")]]]))

(defn handle-search-results
  "Search results are available in the session.
  This will display a two column page."

  [parms]
  (info "handle-search-results " parms)

  (let [results (session-get :search-results)]
    (info "search-consenters/handle-search-results returned "  results)
   (if (empty? results)
       (helper/flash-and-redirect :flash-no-consenters-match-search "/view/select/consenter")
       (helper/rpms2-page-two-column
           [:div#search-consenter-list
             [:ul.search-results
                 (for [{mrn :medical-record-number, fname :first-name, ln :last-name, :as user} results]
                      [:li {:onclick "org.healthsciencessc.rpms2.core.consenter_search_result_clicked(this)"
                            :data-user (pr-str user)} fname " " ln 
                      [:div.secondary "MRN: " mrn ] ])]]
                      (consenter-details) 
           :title (str (i18n :hdr-search-consenter-results))))))

(defn get-view
  [ctx]
  (debug "GET VIEW SEARCH CONSENTER ctx " ctx)
  (handle-search-results (:query-params ctx)))

(defn post-view
  [ctx]
  (debug "POST VIEW SEARCH CONSENTER ctx " ctx)
  (handle-search-results (:body-params ctx)))

;(debug! handle-search-results)
