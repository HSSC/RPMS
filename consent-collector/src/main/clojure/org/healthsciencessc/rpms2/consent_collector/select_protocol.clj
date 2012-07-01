(ns org.healthsciencessc.rpms2.consent-collector.select-protocol
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only [session-get 
                                         session-put!  ]])
  (:use [clojure.tools.logging :only (debug warn info error)])
  (:use [clojure.string :only (split)])
  (:use [clojure.pprint :only (pprint)])
  (:use [org.healthsciencessc.rpms2.consent-collector.debug :only [debug! pprint-str]])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

(defn- checkbox-map
  [cbname selected]

  (merge {:type "checkbox" 
          :name cbname
          :id cbname
          :class "custom"
          } (if selected {:checked "checked" } {}))) 

(defn- select-language
  "Displays radio buttons to select language."
  [langs]
  [:fieldset {:data-role "controlgroup" }
     [:div.sectionlegend "Select Language:" ]
        (for [l langs] 
           (list [:input (merge {:type "radio" 
                                 :name "sp-language" 
                                 :id (:name l)
                                 :value (:id l)
                                } 
                                (if (= "English" (:name l)) {:checked "checked" } {})) ]
                   [:label {:for (:name l) } (:name l) ]  )) ])



(defn- protocol-item
  "Checkbox for a protocol"
  [protocol-version]
 (let [p (:protocol protocol-version)
       nm (:id protocol-version)]
   (list 
     (if (:required p) 
      [:input (merge (checkbox-map nm true) {:disabled "disabled"}) ]
      [:input  (checkbox-map nm (:select-by-default p)) ])
     [:label {:for nm } (:name p) ])))

(defn view 
  "Display form to select protocols and the language to be used. 
  User will select one language which will be applicable to all protocols." 

  [_]
  (helper/rpms2-page 
    (helper/post-form "/view/select/protocols"
      [:div 
       (list 
         (let [plist (dsa/get-published-protocols)
               languages (distinct (apply concat (map :languages plist)))
               meta-data (distinct (apply concat (map :meta-items plist)))
               ]
           (do
             ;;(spit "pubprot.txt" (pprint-str plist))
             (session-put! :protocol-versions plist)
              (list  
                 [:fieldset {:data-role "controlgroup" }
                   [:div.sectionlegend "Select " (helper/org-protocol-label) "(s):" ]
                   (map protocol-item plist)]
                   (select-language languages )))))]
      [:div.centered {:data-role "fieldcontain" }
       (helper/submit-btn { :value "Back" :name :go-back })
       (helper/submit-btn { :value "Continue" :name "select-protocols-form"  })
      ])
    :title (str "Select " (helper/org-protocol-label)) 
    :cancel-btn (helper/cancel-form "/view/select/consenter")
    ))

(defn- needed-pv-ids
  "Returns selected protocol version ids along
  with any required protocol versions. "
  [checkboxmap]
  (let [protocol-versions (session-get :protocol-versions)
        required-protocol-version-ids (->> protocol-versions
                                        (remove #(= false (get-in % [:protocol :required])))
                                        (map :id))
        selected-ids (map name (keys checkboxmap))]
    (distinct (concat required-protocol-version-ids selected-ids))))

(defn- is-go-back?
  "Returns true if the current request represents go-back"
  [ctx]

  (let [bp (:body-params ctx)
        goback (:go-back bp)]
  (not (empty? goback)) ))

(defn- perform-select-protocols
  "Once protocols are selected, update 
  :protocol-versions to contain only the selected protocols
  and go to meta-data page. "

  [ctx]
  (let [needed (set (needed-pv-ids (dissoc (:body-params ctx)
                                       :sp-language
                                       :select-protocols-form)))
        protocols (session-get :protocol-versions)
        needed-protocols (filter #(contains? needed (:id %))
                                 protocols )
        ]
    (do
      (session-put! :selected-protocol-version-ids (vec needed))
      (session-put! :selected-language (-> ctx :body-params :sp-language))
      ;; this removes any unselected protocols from the session
      (debug "# Needed pvs " (count needed-protocols))
      
      (session-put! :protocol-versions  needed-protocols)
      (debug "AFTER FILTERING NUM PROTOCOL VERSIONS "
               (count (session-get :protocol-versions)))
      
      (debug "perform need-protocols " (pprint-str needed-protocols) )
      (debug "perform selected protocols " (pprint-str needed) )
      (debug "perform selected language " (:sp-language ctx))
      (helper/myredirect "/view/meta-data")))) 

(defn perform
  "Either go back or go forward. Go back to the search results page"
  [ctx] 

  (if (is-go-back? ctx )
      (do 
        ;; do not want to reperform the search.
        (debug "GO BACK LAST: " (session-get :last-page))
        (helper/clear-consenter)
        (helper/myredirect "/view/select/consenter"))
      (perform-select-protocols ctx)))

