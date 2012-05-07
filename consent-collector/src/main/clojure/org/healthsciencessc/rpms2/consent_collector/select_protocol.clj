(ns org.healthsciencessc.rpms2.consent-collector.select-protocol
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

(defn form-select-protocols 
  "Form to select protocols. Displays required protocols, followed by the optional protocols."
  [ctx]
  
  (let [ protocols (dsa/get-protocols) 
	 required-protocols (remove #(= (:required %) false) protocols)
	 optional-protocols (remove #(= (:required %) true) protocols) ] 

    [:div.standardForm 
      [:form {:method "GET" :action (helper/mypath "/view/meta-data") }

	;; required forms first 
 	[:div.areaTitle (i18n :select-protocols-form-required-protocols-legend) ]
        (for [protocol required-protocols] 
		[:p (:name protocol)  (:description protocol) ]) 

	[:div (if (empty? required-protocols) "No Required Protocols") ]
 	[:div.areaTitle (i18n :select-protocols-form-optional-protocols-legend) ]

	;; now optional forms 
        [:fieldset {:data-role "controlgroup" }
 	;;[:legend (i18n :select-protocols-form-legend) ]
         
        (for [protocol optional-protocols] 
           (let [n (:name protocol)
                 nm (str "sp-choice-" (:name protocol))]
	    [:div [:input {:name "location" 
			   :id nm 
                           :type "checkbox" 
                           :value n
			   :checked (:select-by-default protocol) } ]
		  [:label {:for nm } (str n " " (:description protocol) )] ])) ]
	[:div.centered (helper/submit-button "select-protocols-form") ]
     ]]
))

(defn view 
  "Returns view "
  [ctx]
  (helper/rpms2-page (form-select-protocols ctx) :title (i18n :hdr-select-protocols)))

(defn perform
  "Performs...  "

  [{{:keys [userid password]} :body-params} ]
  ;;dsa/post-security-authenticate
  (debug "perform-not done")
  (helper/myredirect "/view/select/location"))


