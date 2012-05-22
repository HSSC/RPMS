(ns org.healthsciencessc.rpms2.consent-collector.select-protocol
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug warn info error)])
  (:use [clojure.string :only (split)])
  (:use [clojure.pprint :only (pprint)])
  (:use [org.healthsciencessc.rpms2.consent-collector.debug :only [debug! pprint-str]])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

(defn- find-published 
   "Returns published protocol for protocol p."
   [p published-protocols]

   (first (filter #(= (:id (:protocol %)) 
                      (:protocol-id p) ) published-protocols)))

 (defn- get-control
   "Finds the specified protocol in the published protocol list
   and returns a map with the properties needed to display the 
   selections."
   [p published-protocols]

   (if-let [published (find-published p published-protocols)]
       {:protocol-id (:protocol-id p)
        :name (:name p)
        :languages (:languages (:protocol published))
       }
     (do 
       (warn "CANNOT FIND PUBLISHED Protocol for " p) 
       {})
     ))

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
     ;;[:legend "Select language: " ]
     [:div.sectionlegend "Select language:" ]
        (list (for [l langs] 
           (list [:input (merge {:type "radio" 
                   :name (:name l)
                   :id (:name l)
                   :value (:name l)
                   } (if (= "English" (:name l)) {:checked "checked" } {})) ]
                   [:label {:for (:name l) } (:name l) ]  ))) ])



(defn- select-protocol-control
  [plist]

  [:fieldset {:data-role "controlgroup" }
     [:div.sectionlegend "Select Protocols:" ]
     (list (for [p plist]
        (list (let [ cbname (str "cb-" (:protocol-id p)) ]
                   (list (if (:required p) 
                             [:input (merge (checkbox-map cbname true) {:disabled "disabled"}) ]
                             [:input  (checkbox-map cbname (:select-by-default p)) ])
                          [:label {:for cbname } (:name p) ])))))])

(defn view 
  "Form to select protocols." 
  [ctx]
  (helper/rpms2-page 
    (helper/post-form "/view/select/protocols"
      [:div 
       (list 
         (let [protlist (dsa/get-protocols)
               publist (dsa/get-published-protocols) ]
           (do
                 (session-put! :published-protocols publist)
                 (session-put! :protocols protlist)
               (list  
                 (select-protocol-control protlist)
                 (select-language (dsa/get-languages) )))
        ))]
      [:div.centered {:data-role "fieldcontain" }
       (helper/standard-submit-button { :value "Back" :name :go-back })
       (helper/standard-submit-button { :value "Continue" :name "select-protocols-form"  })
      ])
    :title (i18n :hdr-select-protocols)))

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
        (debug "NNN needed-protocol-ids RETURNING " resp)
        resp))

(defn- my-find-published
  [rid]
  ;publist (session-get :published-protocols) 
  (filter #(= rid (get-in % [:protocol :id])) (session-get :published-protocols) ))

(defn- find-meta-data-items
  "Returns a list with the meta data items for  each of the protocols in publist.
  TODO: each item in the list is a vector, that should be flattened "
  [orig]
  (let [publist (flatten orig)]
  (distinct (flatten (map (fn [n] (do 
                   (get-in n  [:meta-items]))) (flatten publist) )))))

(defn- is-go-back?
  "Returns true if the current request represents go-back"
  [ctx]

  (let [ bp (:body-params ctx)
        goback (:go-back bp)  ]
  (not (empty? goback)) ))

(defn- perform-select-protocols
  "If the back button was pressed, then go back to previous page.
  Once protocols are selected, identify required meta-data items
  and go to the meta-data page."

  [ctx]
  (let [needed (needed-protocol-ids (:body-params ctx))
        protocols-to-be-filled-out  (map my-find-published needed)
        metadata (find-meta-data-items protocols-to-be-filled-out)]
    (session-put! :needed-meta-data metadata)
    (flash-put! :needed-meta-data metadata)
    (session-put! :protocols-to-be-filled-out protocols-to-be-filled-out)
    (session-put! :current-step "Form-1-Page-1")

    (debug "Protocols to be filled out " (pprint-str needed) " Num meta " (count metadata)) 
    (helper/myredirect "/view/meta-data"))) 

(defn perform
  "Either go back or go forward. Go back to the search results page"
  [ ctx ] 

  (if (is-go-back? ctx )
      (do 
        ;; do not want to reperform the search.
        (debug "GO BACK LAST: " (session-get :last-page))
        (helper/myredirect "/view/select/consenter"))
      (perform-select-protocols ctx)))

;;(debug! perform)
