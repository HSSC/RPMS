(ns org.healthsciencessc.rpms2.consent-collector.create-consenter
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n i18n-label-for i18n-placeholder-for]]))

(defn view 
  "Returns form to create a consenter."
  [ctx]
  (helper/rpms2-page 
    (helper/post-form "/view/create/consenter" 
      (list 
      [:div.left "Present form for the user fill out to Create consenter" ]
         (for [v dsa/create-consenter-fields] 
             (list 
               (helper/emit-field-def (dsa/consenter-field-defs v) :create-consenter-form (name v) "editable" ))))

        (list [:div.centered
           (helper/submit-button "create-consenter-form" 
             (i18n "create-consenter-form-submit-button" ) "create-consenter") ]))

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

