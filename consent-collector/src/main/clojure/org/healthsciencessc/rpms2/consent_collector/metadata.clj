(ns org.healthsciencessc.rpms2.consent-collector.metadata
  "Processes for the meta data view and actions. "
  (:require ( [org.healthsciencessc.rpms2.consent-collector [helpers :as helper]
                                                         [factories :as factory]]))
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.factories 
          :only (generate-meta-data-items)])
  (:use [org.healthsciencessc.rpms2.consent-collector.helpers 
          :only (mypath placeholder-kw submit-button rpms2-page myredirect)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only (i18n)]))


(defn- name-value-bold-input
   "Creates a div with a label and name, which will be horizontally styled.
    Setting data-role to 'fieldcontain' tells jquery mobile to group the 
    label and the value and display horizontally if possible."
   [form-name v id]

   [:div.valueimportantblock {:data-role "fieldcontain" } 
     [:label {:for v :class "labeldim" } (i18n form-name v "label") ]
     [:div.highlightvalue { :id id } ]])

(defn form-meta-data
  "Displays a form for the user to enter the meta data items 
  that are required."
  [ctx]

  [:div.standardForm [:div.areaTitle (i18n :meta-data-form-section-label ) ]
    (let [a (generate-meta-data-items) form-name "meta-data-form" ]
    [:form {:method "GET" :action 
            (mypath "/view/select/protocols" ) } 
          (for [item a] 
               [:div 
		;; when type is string then display a text field
		;;(pr-str "<br/><b>" (:name item) "</b> type " (:data-type item)) 
		;;[:label (pr-str "<br/>Meta data <b>" (:name item) "</b> type " (:data-type item)) ]
		(when (= (:data-type item) "date") 
			[:div
			(pr-str "<br/><b>" (:name item) "</b> type " (:data-type item)) 
			(name-value-bold-input form-name (:name item) (:name item))
			[:div [:label (:name item) ] [:input { :type "date" } ]]])
		(when (= (:data-type item) "string") 
		   [:div 
   			[:div.valueimportantblock {:data-role "fieldcontain" } 
[:label {:for (:name item) :class "labeldim" } (i18n form-name (:name item) "label" ) ]
			[:input { :type "text" :value (i18n (placeholder-kw form-name (:name item))) :name (:name item)} ] ]
			]
		)
 	   ]) 
       ])
   [:div (submit-button "meta-data-form") ] ])

(defn view 
  "Returns meta data form"
  [ctx]
  (rpms2-page (form-meta-data ctx) :title (i18n :hdr-metadata)))

(defn perform
  "Performs...  "

  [{{:keys [userid password]} :body-params} ]
  ;;dsa/post-security-authenticate
  (debug "perform-not done")
  (myredirect "/view/select/location"))


