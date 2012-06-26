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
        (list (for [l langs] 
           (list [:input (merge {:type "radio" 
                                 :name "sp-language" 
                                 :id (:name l)
                                 :value (:name l)
                                } 
                                (if (= "English" (:name l)) {:checked "checked" } {})) ]
                   [:label {:for (:name l) } (:name l) ]  ))) ])



(defn- protocol-item
  "Checkbox for a protocol"
  [p]

 (let [ nm (str "cb-" (:protocol-id p)) ]
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
         (let [data (dsa/get-available-protocols-and-languages)
               plist (:available-protocols data) ]
           (do
              (session-put! :protocols plist)
              (list  
                 [:fieldset {:data-role "controlgroup" }
                   [:div.sectionlegend "Select " (helper/org-protocol-label) "(s):" ]
                   (map protocol-item plist)]
                   (select-language (:languages data) )))))]
      [:div.centered {:data-role "fieldcontain" }
       (helper/submit-btn { :value "Back" :name :go-back })
       (helper/submit-btn { :value "Continue" :name "select-protocols-form"  })
      ])
    :title (str "Select " (helper/org-protocol-label)) 
    :cancel-btn (helper/cancel-form "/view/select/consenter")
    ))

(defn- needed-protocol-ids
  "Returns protocol ids that need to be filled out.  This is the required
  forms plus any protocols selected by the user (specified by a parameter named cb-<ProtocolID>)."
  [cbmap]

  (let [required-protocols (remove #(= (:required %) false) (session-get :protocols))
        required-ids (map :protocol-id required-protocols)
        selected-ids (->> (filter #(.startsWith (name %) "cb-" )(keys cbmap)) 
                          (map name)
                          (map (fn [n] (split n #"-")))
                          (map last)) 
        resp  (flatten (conj required-ids selected-ids)) ]
        resp))

(defn- is-go-back?
  "Returns true if the current request represents go-back"
  [ctx]

  (let [bp (:body-params ctx)
        goback (:go-back bp)]
  (not (empty? goback)) ))

(defn- perform-select-protocols
  "Once protocols are selected, identify required meta-data items
  and go to meta-data page. "

  [ctx]
  (let [needed (needed-protocol-ids (:body-params ctx))]
    (do
      (session-put! :needed-protocol-ids needed)
      (session-put! :selected-language (:sp-language ctx))
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

