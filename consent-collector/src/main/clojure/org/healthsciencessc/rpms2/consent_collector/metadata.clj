(ns org.healthsciencessc.rpms2.consent-collector.metadata
  "Processes for the meta data view and actions. "
  (:require ( [org.healthsciencessc.rpms2.consent-collector [helpers :as helper]
                                                         [factories :as factory]]))
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.factories 
          :only (generate-meta-data-items)])
  (:use [org.healthsciencessc.rpms2.consent-collector.helpers 
          :only (mypath submit-button rpms2-page myredirect)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only (i18n)]))

(comment
  (def ^:private md-i18n
    "meaningless docstring"
    (partial i18n :metadata-form)))

(defn form-meta-data
  "Displays a form for the user to enter the meta data items 
  that are required."
  [ctx]

  (let [md-i18n (partial i18n :meta-data-form)]
    [:div.standardForm [:div.areaTitle (md-i18n :section-label ) ]
     [:form {:method "GET" :action 
             (mypath "/view/select/protocols" ) } 
      (for [{nm :name :as item} (generate-meta-data-items)]
        [:div 
         ;; when type is string then display a text field
         ;;(pr-str "<br/><b>" (:name item) "</b> type " (:data-type item)) 
         ;;[:label (pr-str "<br/>Meta data <b>" (:name item) "</b> type " (:data-type item)) ]
         (when (= (:data-type item) "date") 
           [:div
            (pr-str "<br/><b>" nm "</b> type " (:data-type item))
            [:div.valueimportantblock {:data-role "fieldcontain" } 
             [:label {:for nm :class "labeldim" } (md-i18n nm "label") ]
             [:div.highlightvalue { :id nm } ]]
            [:div [:label nm] [:input { :type "date" } ]]])
         (when (= (:data-type item) "string") 
           [:div 
            [:div.valueimportantblock {:data-role "fieldcontain" } 
             [:label {:for nm :class "labeldim" } (md-i18n nm "label" ) ]
             [:input { :type "text" :value (md-i18n nm :placeholder) :name nm}]]])])]
     [:div (submit-button "meta-data-form")]]))

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


