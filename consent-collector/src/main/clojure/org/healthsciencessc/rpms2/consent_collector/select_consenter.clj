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
           (helper/submit-button "search-consenters" 
                   (i18n "search-consenters-form-submit-button" ) 
                   "search-consenters")
            (helper/submit-button "search-consenters" 
                   (i18n "create-consenter-form-submit-button") 
                   "create-consenter") ]]] 

    :title (i18n :hdr-search-consenters)))
  
;(debug! view)

(defn perform
  "Either create a new consenter or go to search page.
  If search-consenters parameter has a value, then perform a search.
  Otherwise, create a new consenter."
  [ { {:keys [search-consenters create-consenter]} :body-params :as ctx} ]
  (if search-consenters 
      (let [org-id (get-in (session-get :org-location) [:organization :id])
              {status :status results :json} (dsa/dsa-search-consenters (ctx :body-params) org-id)]
          (info "select_consenter/perform response from search is " results " status is " status  )
          (if (= status 200) 
              (do 
                  (session-put! :search-results results)
                  (helper/myredirect "/view/search/consenters"))
              (if (= status 404) 
                  (helper/flash-and-redirect (str "Nothing matches those criteria.  Try again or create a new consenter.") "/view/select/consenter")
                  (helper/flash-and-redirect (str "problem with search " status) "/view/select/consenter"))
                ))
      (helper/myredirect "/view/create/consenter"))) 

;(debug! perform)
