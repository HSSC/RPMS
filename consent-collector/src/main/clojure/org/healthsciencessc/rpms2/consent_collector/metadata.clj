(ns org.healthsciencessc.rpms2.consent-collector.metadata
  "Processes for the meta data view and actions. "
  (:require ( [org.healthsciencessc.rpms2.consent-collector [helpers :as helper]]))
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.dsa-client
          :only (generate-meta-data-items)])
  (:use [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only (i18n)]))

(defn form-meta-data
  "Displays a form for the user to enter the meta data items 
  that are required."
  [ctx]

  (let [md-i18n (partial i18n :meta-data-form)
        items (generate-meta-data-items) 
        items-per-col 5
        nitems (count items)
        ncols (+
                (quot nitems items-per-col)
                (if (> 0 (rem nitems items-per-col)) 1 0))
        _ (println "num items " (count items) " num columns " ncols)
        ]
    (helper/post-form "/view/unimplemented"
       (list (for [{nm :name :as item} items]
        (do (println "META " nm) 
          (list [:div 
         ;; when type is string then display a text field
         ;;(pr-str "<br/><b>" (:name item) "</b> type " (:data-type item)) 
         ;;[:label (pr-str "<br/>Meta data <b>" (:name item) "</b> type " (:data-type item)) ]
         (cond 
           (= (:data-type item) "date")
           [:div
            (println "DATE")
            (pr-str "<br/><b>" nm "</b> type " (:data-type item))
            [:div.valueimportantblock {:data-role "fieldcontain" } 
             [:label {:for nm :class "labeldim" } (md-i18n nm "label") ]
             [:div.highlightvalue { :id nm } ]]
            [:div [:label nm] [:input { :type "date" } ]]]

           (= (:data-type item) "string") 
           [:div 
             (println "STRING " item)
            [:div.valueimportantblock {:data-role "fieldcontain" } 
             [:label {:for nm :class "labeldim" } (md-i18n nm "label" ) ]
             [:input { :type "text" :value (md-i18n nm :placeholder) :name nm}]]]

           :else
           [:div [:h1 "other" item ]]
           )
         ]))))
       (helper/standard-submit-button { :value (i18n :meta-data-form-submit-button) } )))

    #_[:div.centered
     [:form {:method "GET" :action 
             (mypath "/view/unimplemented" ) } 
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
     [:div.centered (submit-button "meta-data-form")]])
;)

(defn view 
  "Returns meta data form"
  [ctx]
  (rpms2-page (form-meta-data ctx) :title (i18n :hdr-metadata)))

