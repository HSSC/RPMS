(ns org.healthsciencessc.rpms2.consent-collector.select-consenter
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper]
   [org.healthsciencessc.rpms2.process-engine.core :as process])

  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [org.healthsciencessc.rpms2.consent-collector.debug :only [debug!]]) 
  (:use [clojure.tools.logging :only (debug info warn error)])
  (:use [clojure.pprint])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

(defn view 
   "Returns form to search consenter and a button to create consenter"
  [ctx]

  (helper/rpms2-page 
     [:div  [:form {:method "POST" 
             :action (helper/mypath "/view/select/consenter") 
             :data-ajax "false" }

        (for [s dsa/consenter-fields]
              (helper/text-field3 "search-consenters-form" (name s)))

        [:div.centered  {:data-role "fieldcontain" } 

          [:input {:type "submit" 
                   :data-theme "a"
                   :data-role "button"
                   :data-inline "true "
                   :value (i18n "search-consenters-form-submit-button")
                   :name "search-consenters" } ]

          [:input {:type "submit" 
                   :data-theme "a"
                   :data-role "button"
                   :data-inline "true "
                   :value (i18n "select-consenters-view-create-consenter-button")
                   :name "create-consenters" } ]]]] 

    :title (i18n :hdr-search-consenters)))


(defn- perform-search
   [ctx]
   (let [org-id (get-in (session-get :org-location) [:organization :id])
          _ (debug "perform-search org id " org-id)
          _ (debug "org id " org-id)
          response  (dsa/dsa-search-consenters (ctx :body-params) org-id)
          _ (debug "response"  response)
          status (:status response)
          _ (debug "status "  status )
          json (:json response)
          _ (debug "json "  json )
          results json
          _ (debug "results  "  results )
         ]

    (info "perform-search response " results " status is " status  )
    (if (or (= status 200) 
            (= status 302))
         (do 
            (session-put! :search-results results)
            (helper/myredirect "/view/search/consenters"))
         (if (= status 404) 
            (helper/flash-and-redirect (str "Nothing matches those criteria.  Try again or create a new consenter.") "/view/select/consenter")
            (helper/flash-and-redirect (str "problem with search " status) "/view/select/consenter"))
         )))


(defn- is-search?
  "Returns if the current request represents a search."
  [ctx]

  (let [ bp (:body-params ctx)
        do-search (:search-consenters bp)
        do-create (:create-consenter bp) ]

  (debug "is-search? parms: " bp,  " do-search: ",  do-search,  " create: " do-create,  
         " SEARCH IS " (not (empty? do-search)) ) 
  (not (empty? do-search)) ))

(defn perform
  "Either create a new consenter or go to search page.
  If search-consenters parameter has a value, then perform a search.
  Otherwise, create a new consenter."
  [ ctx ] 

  (println "select_consenter/perform SEARCH? " (is-search? ctx ))
  (if (is-search? ctx )
      (perform-search ctx)
      (helper/myredirect "/view/create/consenter")))


(debug! perform)
(debug! perform-search)

