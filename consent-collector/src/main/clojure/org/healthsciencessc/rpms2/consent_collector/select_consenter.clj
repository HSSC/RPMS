(ns org.healthsciencessc.rpms2.consent-collector.select-consenter
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper]
   [org.healthsciencessc.rpms2.process-engine.core :as process])

  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [org.healthsciencessc.rpms2.consent-collector.debug :only [debug!]]) 
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

(def consenter-fields  ["first-name"
                        "last-name"
                        "consenter-id"
                        "date-of-birth"
                        "zipcode"])

#_(defn- search-form
  []

  [:div [:div { :data-user "THIS IS THE DATA THAT IS ON THE DIV" 
   :onclick "org.healthsciencessc.rpms2.core.consenter_search_clicked(this)" } 
    "SUBMIT SEARH BY CLICKING HERE IN THIS AREA" ] 
  (helper/standard-form "POST" (helper/mypath "/view/select/consenter") 
  (for [s consenter-fields]
     (helper/text-field3 "search-consenters-form" s))

     (helper/submit-button "search-consenters-form") 
     [:div#tiny [:form {:method "GET" :action (helper/mypath "/view/create/consenter") } 


  (let 
    [
     form-name "search-consenters-form"
     n1 (keyword (str "search-consenters-form-submit-button" ))
     n2 (keyword (str form-name "-submit-button" ))
     ]

    (submit-button "search-consenters" (i18n n1) (str form-name "-submit-button") )

    (submit-button "search-consenters" (i18n n2) "create-consenter-form-submit-button" )

    [:input 
      {:type "submit" 
       :data-theme "a"
       :data-role "button"
       :data-inline "true"
       :value (i18n n1) 
       :name (str form-name "-submit-button") } ]

    [:input 
      {:type "submit" 
       :data-theme "a"
       :data-role "button"
       :data-inline "true"
       :value (i18n n2) 
       :name (str "create-consenter-form-submit-button") } ]
    )

     ;;(helper/submit-button "create-consenter-form") 
     ;;(helper/ajax-submit-button "ajax-search-consenters-form") 
                 ]] )])


; (debug! search-form)

(defn view 
   "Returns form to search consenter and a button to create consenter"
  [ctx]

  (helper/rpms2-page 
     [:div  [:form {:method "POST" 
             :action (helper/mypath "/view/select/consenter") 
             :data-ajax "false" }

        (for [s consenter-fields]
              (helper/text-field3 "search-consenters-form" s))

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
  "Either create a new consenter or go to search page."
  [ { {:keys [search-consenters create-consenter]} :body-params :as ctx} ]
  ;;(println (str "PERFORM===> ARGS IS " ctx) )
  (if search-consenters 
      (do 
        (process/dispatch "post-view-search-consenters" ctx ))
        (helper/myredirect "/view/create/consenter")))


;(debug! perform)
