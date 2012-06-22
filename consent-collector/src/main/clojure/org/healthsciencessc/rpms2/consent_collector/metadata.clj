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
  (:use [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only (i18n)]))

(defn- date-field
  [nm item idval]

  [:div.inputdata {:data-role "fieldcontain" } 
      [:label {:for idval :class "labelclass" } nm ]
      [:input { :type "date" :class "inputclass" :id idval } ]])

(defn- string-field
  [nm item idval]

  [:div.inputdata {:data-role "fieldcontain" } 
      [:label {:for idval :class "labelclass" } nm  ]
      [:input {:type "text" 
               :name idval
               :id idval 
               :class "inputclass" }]])

(defn- yes-no-field
  [nm item]

  [:div.inputdata {:data-role "fieldcontain" } 
     [:label {:for nm :class "labelclass" } "CHECKBOX " nm  ]
     [:input { :type "checkbox" :id nm :name nm :class "inputclass" }] "CHECKBOX  " nm ])

(defn- other-field
  [nm item]

  [:div.inputdata {:data-role "fieldcontain" }  "OTHER" nm ])
     
(defn- emit-item
  [item]

  (cond   (or (= (:data-type item) "date")
              (=  (:data-type item) "xsd:date"))
          (date-field (:name item) item
                        (or (:mdid item) 
                            (:name item)))
            
          (or (= (:data-type item) "string") 
              (= (:data-type item) "xsd:string"))
          (string-field (:name item) item
                        (or (:mdid item) 
                            (:name item)))

          (= (:data-type item) "yes-no") 
          (yes-no-field (:name item) item)

          :else
          (other-field (:name item) item)))

(defn view 
  "Returns Consenter Information ( meta data ) form"
  [ctx]
  (rpms2-page 
    (helper/post-form "/view/meta-data"
     (list [:div.left "Enter the following information:" ]
       (list (for [item (session-get :needed-meta-data)]
            (list (emit-item item)))))
       (helper/submit-btn { :value (i18n :meta-data-form-submit-button) } ))
    :title (i18n :hdr-metadata)
    :cancel-btn (helper/cancel-form "/view/select/consenter")))

(defn perform
  "Save meta data and prepare to enter the data."
  [{parms :body-params :as ctx}]

  (helper/myredirect "/collect/consents"))
