(ns org.healthsciencessc.rpms2.consent-collector.search-consenter
  (:require
   [org.healthsciencessc.rpms2.consent-collector [ dsa-client :as dsa]
    [helpers :as helper]
    [select-consenter :as sc]
    [factories :as factory]]
   [clojure.string :as s])
  (:use [org.healthsciencessc.rpms2.consent-collector.debug :only [debug!]])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))


(defn- remove-blank-vals
  "Given a map, removes all key/val pairs for which the value
  is blank."
  [m]
  (into {}
        (for [[k v] m :when (not (s/blank? v))]
          [k v])))

(defn search-consenters
  [params]
  (let [consenter-params (remove-blank-vals
                          (select-keys params
                                       (map keyword sc/consenter-fields)))
        org-id  (get-in (session-get :org-location) [:organization :id])]
    ;; if no params specified, then call get-consent-consenters
    ;; otherwise all get-consent-consenter (which is a search)
    (if (empty? consenter-params)
      (dsa/dsa-call :get-consent-consenters {:organization org-id})
      (dsa/dsa-call :get-consent-consenter (assoc consenter-params :organization org-id)))))

#_(defn search-consenters
    [params]
    {:status 200
     :json
     (vec (factory/generate-user-list params))})

(debug! search-consenters)


(defn consenter-details
  "This is right hand side of the search results page, showing
  the details associated with the selected consenter (e.g. patient)"
  []
  (let [scrf-i18n (partial i18n :search-consenter-results-form)]
    [:div#consenter-details
     [:h1 (scrf-i18n :verify-record-details)]
     [:ul (for [v [ "visit-number" 
                    "medical-record-number" 
                    "encounter-date" ]]
            [:li {:data-role "fieldcontain" } 
             [:label {:for v :class "labeldim" } (scrf-i18n v :label) ]
             [:div.highlightvalue { :id (str "consenter-" v ) } "..." ]])]

     [:h1 (scrf-i18n :verify-patient-details)]
     [:ul (for [v [ "name"
                    "date-of-birth"
                    "last-4-digits-ssn"
                    "zipcode"
                    "referring-doctor"
                    "primary-care-physician"
                    "primary-care-physician-city" ]]
            [:li {:data-role "fieldcontain" }
             [:label {:for v :class "labeldim" } (scrf-i18n v :label) ]
             [:div.highlightvalue { :id (str "consenter-" v ) } "..." ]]) ]

     ;; section for "Is this correct?" with Yes/No buttons
     [:h1.centered (scrf-i18n :confirmation-question )]

     ;; save current selection 
     [:form#other-section { :method "GET" :action (helper/mypath "/view/search/results") } 
      [:input {:type "hidden" :name "patient-id" :id "patient-id" :value "no patient" } ]
      [:input {:type "hidden" :name "patient-name" :id "patient-name" :value "no patient" } ]
      [:input {:type "hidden" :name "patient-encounter-data" :id "patient-name" :value "no patient" } ]
      (helper/submit-button "search-consenter-results-yes") 
      (helper/submit-button "search-consenter-results-no")]]))

(defn- flash-and-redirect
  [i18n-key path]
  (flash-put! :header (i18n i18n-key))
  (helper/myredirect path))

(defn process-search-consenters
  ;; TODO: add the org/loc from the session into the
  ;; query params; remove the submit button
  ;; maybe use a filter to select the correct parameters
  ;; and add in user defaults
  [parms]
  (let [{status :status results :json} (search-consenters parms)]
    (cond
     
     (= 401 status)
     (flash-and-redirect :flash-service-failure "/view/select/consenter")

     (empty? results)
     (flash-and-redirect :flash-no-consenters-match-search "/view/select/consenter")

     :else
     (helper/rpms2-page-two-column
      [:div#search-consenter-list
       [:ul.search-results
        (for [{mrn :medical-record-number, fname :first-name, ln :last-name, :as user} results]
          [:li {:onclick "org.healthsciencessc.rpms2.core.consenter_search_result_clicked(this)"
                :data-user (pr-str user)} fname " " ln 
           [:div.secondary "MRN: " mrn ] ])]]
      (consenter-details) 
      :title (i18n :hdr-search-consenter-results)))))

(defn get-view
  [ctx]
  (process-search-consenters (:query-params ctx)))

(defn post-view
  [ctx]
  (process-search-consenters (:body-params ctx)))

