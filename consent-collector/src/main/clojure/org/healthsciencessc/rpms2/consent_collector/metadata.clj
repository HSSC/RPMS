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
  "Displays a form for user to enter required meta data items."
  [ctx]

  (let [md-i18n (partial i18n :meta-data-form)
        items (generate-meta-data-items) 
        items-per-col 5
        nitems (count items)
        ncols (+
                (quot nitems items-per-col)
                (if (> 0 (rem nitems items-per-col)) 1 0))
        _ (println "num items " nitems " num columns " ncols  
                " QUOT " (quot nitems items-per-col)
                " REM  " (rem nitems items-per-col))
        _ (debug "num items " nitems " num columns " ncols  
                " QUOT " (quot nitems items-per-col)
                " REM  " (rem nitems items-per-col)
                 " SUM " (+
                (quot nitems items-per-col)
                (if (> 0 (rem nitems items-per-col)) 1 0))
                 )
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
            [:div.valueimportantblock {:data-role "fieldcontain" } 
             [:label {:for nm :class "labeldim" } (md-i18n nm "label" ) ]
             [:input { :type "text" :value (md-i18n nm :placeholder) :name nm}]]

           (= (:data-type item) "yes-no") 
            [:div.valueimportantblock {:data-role "fieldcontain" } 
             [:label {:for nm :class "labeldim" } "CHECKBOX " nm  ]
             [:input { :type "checkbox" :id nm :name nm}] "CHECKBOX  " nm ]

           :else
           [:div "other" item ]
           )
         ]))))
       (helper/standard-submit-button { :value (i18n :meta-data-form-submit-button) } ))))


(defn view 
  "Returns meta data form"
  [ctx]
  (rpms2-page (form-meta-data ctx) :title (i18n :hdr-metadata)))

