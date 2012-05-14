(ns org.healthsciencessc.rpms2.consent-collector.select-protocol
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

(defn- emit-required
  "Generates required protocols section."

  [required-protocols]
  (list [:div.areaTitle (i18n :select-protocols-form-required-protocols-legend) ]
        (for [protocol required-protocols] 
		[:p (:name protocol)  (:description protocol) ]) 
	[:div (if (empty? required-protocols) "No Required Protocols") ]
 	[:div.areaTitle (i18n :select-protocols-form-optional-protocols-legend) ]))

(defn- emit-optional
  "Generates optional protocols section."
  [optional-protocols]

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
		  [:label {:for nm } (str n " " (:description protocol) )] ])) ])


(defn view 
  "Form to select protocols. Displays required protocols, followed by optional protocols."
  [ctx]
  (helper/rpms2-page 
    (helper/post-form "/view/select/protocols"
      (list (let [protocols (dsa/get-protocols) ] 
         (list (emit-required (remove #(= (:required %) false) protocols))
               (emit-optional (remove #(= (:required %) true) protocols))))
	 [:div.centered (helper/submit-button "select-protocols-form") ]))
    :title (i18n :hdr-select-protocols)))

(defn perform
  "Performs...  "

  [{{:keys [userid password]} :body-params} ]
  ;;dsa/post-security-authenticate
  (debug "select-protocol/perform not done")
                                                     ;; select protocol ids
  (let [ protocol-details (dsa/get-protocols-version '() ) ] 
      (debug "protocol details "  protocol-details))
  (helper/myredirect "/view/meta-data"))


