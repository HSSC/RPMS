(ns org.healthsciencessc.rpms2.consent-collector.create-consenter
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

#_(def create-consenter-fields [ "first-name"
                               "middle-name"
                               "last-name"
                               "local-identifier"
                               "local-identifier-type"
                               "gender"
                               "race"
                               "religion"
                               "address"
                               "phone"
                               "email"
                               "date-of-birth" ])


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
          (for [v dsa/create-consenter-fields ] 
             (helper/text-field3 "create-consenter-form" (name v)))]
        [:div.centered
           (helper/submit-button "create-consenter-form" 
             (i18n "create-consenter-form-submit-button" ) "create-consenter") ]]]
     :title (i18n :hdr-create-consenter)))

(defn perform
  "Saves consenter and if successful, goes to /view/select/protocol.
  If insufficient privileges, displays flash"

  [ctx]
  (let [parms (:body-params ctx)]
    (println "create-consenter/perform Parms " parms)
    ;; should we do any validation before attempt to create the consenter?
    ;;(doall (for [v create-consenter-fields] 
    ;;        (println "the value of " v " is " ((keyword v) parms))))
    ;; specify organization , location as query params,  consenter is body param
    ;; could probably put this in the ring wrapper
    ;; handle standard errors
    (let [resp (dsa/dsa-create-consenter parms) 
          status (:status resp)]
      (if (= 403 (:status resp)) 
         (helper/flash-and-redirect 
            (print-str "You " (helper/username) " are not authorized to create a consenter")
           "/view/create/consenter" ) 
         (if (= 401 (:status resp)) 
           (helper/flash-and-redirect "not logged in" "/view/create/consenter")
          (let [body (:body resp) 
                msg (print-str "Consenter created " body " id=" (:id body)) ]
          (helper/flash-and-redirect msg "/view/select/protocols")))))))

