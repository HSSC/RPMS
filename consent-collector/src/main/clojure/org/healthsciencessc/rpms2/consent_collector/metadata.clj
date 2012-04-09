(ns org.healthsciencessc.rpms2.consent-collector.metadata
  (:require [hiccup.core :as hiccup]
            [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
            [org.healthsciencessc.rpms2.consent-collector.helpers :as helper]
            [org.healthsciencessc.rpms2.consent-collector.factories :as factory]
            [hiccup.page-helpers :as hpages]
            [hiccup.form-helpers :as hform])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))


(defn form-meta-data
  "Displays a form for the user to enter the meta data items 
  that are required."
  [ctx]

  [:div.standardForm [:div.areaTitle (i18n :meta-data-form-section-label ) ]
    (let [a (factory/generate-meta-data-items) 
		form-name "meta-data-form" ]
    [:form {:method "GET" :action (helper/mypath "/view/select/protocols" ) } 
          (for [item a] 
               [:div 
		;; when type is string then display a text field
		;;(pr-str "<br/><b>" (:name item) "</b> type " (:data-type item)) 
		;;[:label (pr-str "<br/>Meta data <b>" (:name item) "</b> type " (:data-type item)) ]
		(when (= (:data-type item) "date") 
			[:div
			(pr-str "<br/><b>" (:name item) "</b> type " (:data-type item)) 
			(helper/name-value-bold-input form-name (:name item) (:name item))
			[:div [:label (:name item) ] [:input { :type "date" } ]]])
		(when (= (:data-type item) "string") 
		   [:div 
   			[:div.valueimportantblock {:data-role "fieldcontain" } 
[:label {:for (:name item) :class "labeldim" } (i18n (helper/label-kw form-name (:name item) )) ]
			[:input { :type "text" :value (i18n (helper/placeholder-kw form-name (:name item))) :name (:name item)} ] ]
			]
		)
 	   ]) 
       ])
   [:div (helper/submit-button "meta-data-form") ] ])

(defn view 
  "Returns meta data form"
  [ctx]
  (helper/rpms2-page (form-meta-data ctx) :title (i18n :hdr-metadata)))

(defn perform
  "Performs...  "

  [{{:keys [userid password]} :body-params} ]
  ;;dsa/post-security-authenticate
  (debug "perform-not done")
  (helper/myredirect "/view/select/location"))


