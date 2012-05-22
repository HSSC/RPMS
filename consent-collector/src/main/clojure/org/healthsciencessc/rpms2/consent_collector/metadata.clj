(ns org.healthsciencessc.rpms2.consent-collector.metadata
  "Processes for the meta data view and actions. "
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! destroy-session! ]])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [clojure.pprint :only (pprint)])
  (:use [org.healthsciencessc.rpms2.consent-collector.debug :only [debug! pprint-str]])
  (:use [org.healthsciencessc.rpms2.consent-collector.dsa-client
          :only (generate-meta-data-items)])
  (:use [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only (i18n)]))


(defn- date-field
  [nm item]

  [:div.valueimportantblock {:data-role "fieldcontain" } 
      [:label {:for nm :class "labeldim" } nm ]
      [:input { :type "date" } ]])

(defn- string-field
  [nm item]

  [:div.valueimportantblock {:data-role "fieldcontain" } 
      [:label {:for nm :class "labeldim" } nm  ]
      [:input { :type "text" :name nm}]])

(defn- yes-no-field
  [nm item]

  [:div.valueimportantblock {:data-role "fieldcontain" } 
     [:label {:for nm :class "labeldim" } "CHECKBOX " nm  ]
     [:input { :type "checkbox" :id nm :name nm}] "CHECKBOX  " nm ])

(defn- drop-down-field
  [nm item]
)


(defn- other-field
  [nm item]

  [:div.valueimportantblock {:data-role "fieldcontain" }  "OTHER" nm ])
     

(defn- emit-item
  [item]

  (cond   (or (= (:data-type item) "date")
              (=  (:data-type item) "xsd:date"))
          (date-field (:name item) item)
            
          (or (= (:data-type item) "string") 
              (= (:data-type item) "xsd:string"))
          (string-field (:name item) item)

          (= (:data-type item) "yes-no") 
          (yes-no-field (:name item) item)

          ;;(= (:data-type item) "dropdown") 
          ;;(dropdown-field (:name item) item)

          :else
          (other-field (:name item) item)))

(defn- emit-hardcoded-items
  []

  [:div.valueimportantblock {:data-role "fieldcontain" } 
  [:label {:for "who-is-signing" :class "labeldim" } "Who is signing the consent" ]
  [:select { :name "who-is-signing"  :id "who-is-signing" }
   [:option {:value "consenter" } "Consenter" ]
   [:option {:value "consenter-rep" } "Consenter Representative" ] ]])

(defn view 
  "Returns meta data form"
  [ctx]
  (rpms2-page 
    (helper/post-form "/view/meta-data"
     (list 
       [:div.left "Enter the following information:" ]
       ;;(emit-hardcoded-items)
       (for [item (flash-get :needed-meta-data)]
            (emit-item item)))
     (helper/standard-submit-button 
        { :value (i18n :meta-data-form-submit-button) } ))
    :title (i18n :hdr-metadata)))

(defn- get-collect-start-page
  [f]
  (debug "get-collect " (:collect-start f))
  (:collect-start f))

(defn perform
  "Save meta data and prepare to enter the data."
  [ctx]
  (debug "Saving meta data and preparing to enter the data.")

  (let [form (dsa/sample-form)
        m {:form form :state :begin :current-page-name (get-collect-start-page form) }]
    (session-put! :collect-consent-status m)
    (helper/myredirect "/collect/consents")))
