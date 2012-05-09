(ns org.healthsciencessc.rpms2.consent-collector.create-consenter
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

(defn view 
  "Returns form to create a consenter."
  [ctx]
  (helper/rpms2-page 
    [:div.innerform 
      [:form {:method "POST"
             :action (helper/mypath "/view/create/consenter")
             :data-ajax "false" } 
      [:div.centered "Present form for the user fill out to Create consenter" ]
      [:div#consenter-details  
         (for [v dsa/create-consenter-fields] 
             (list 
               (let [form-name "create-consenter-form"
                     field-name (name v)

                     i18n-name (:i18n-name (dsa/consenter-field-defs v)) 
                     required (:required (dsa/consenter-field-defs v))
                     specified-kind  (:type dsa/consenter-field-defs v)
                     len (:length (dsa/consenter-field-defs v))

                     kind (if specified-kind specified-kind "text")

                     label (if i18n-name (i18n i18n-name "label") 
                                        (i18n form-name field-name "label" )) 

                     placeholder  (if i18n-name (i18n i18n-name "placeholder") 
                                                (i18n form-name field-name "placeholder" ) )

                     m {:type kind :name field-name :placeholder placeholder } 
                     mp1 (if required (assoc m :required "") m) 
                     input-map (if len (assoc mp1 :length "") mp1) 
                     ]

                     [:div.inputdata  {:data-role "fieldcontain" } 
                       [:label {:for field-name :class "labelclass" } label ]
                       [:input input-map ] ])))]
               

        [:div.centered
           (helper/submit-button "create-consenter-form" 
             (i18n "create-consenter-form-submit-button" ) "create-consenter") ]]]
     :title (i18n :hdr-create-consenter)))

(defn perform
  "Saves consenter and if successful, goes to /view/select/protocol.
  If insufficient privileges or if the fields are not validated, displays flash message."

  [ctx]
  (let [parms (:body-params ctx)]
    (let [resp (dsa/dsa-create-consenter parms) 
          status (:status resp) 
          body (:body resp)]

      (cond (= 403 (:status resp))
            (helper/flash-and-redirect 
                 (print-str "You " (helper/username) " are not authorized to create a consenter")
                 "/view/create/consenter" ) 

            (= 409 (:status resp))
            (helper/flash-and-redirect (:body resp) 
                 "/view/create/consenter" ) 

            (= 401 (:status resp))
                 (helper/flash-and-redirect 
                 "not logged in" 
                 "/view/login")

            :else
                 (helper/myredirect  "/view/select/protocols")))))

