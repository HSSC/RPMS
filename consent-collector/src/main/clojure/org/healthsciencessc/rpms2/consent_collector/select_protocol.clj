(ns org.healthsciencessc.rpms2.consent-collector.select-protocol
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug warn info error)])
  (:use [clojure.string :only (split)])
  (:use [org.healthsciencessc.rpms2.consent-collector.debug :only [debug!]])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

(defn- find-published 
   "Returns the published protocol for protocol p."
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
       (println "CANNOT FIND PUBLISHED " p) 
       (warn "CANNOT FIND PUBLISHED Protocol for " p) 
       {})
     ))


(defn- optional-cb
  "Returns an optional checkbox.
  which may be selected by default"
  [orig cbname]

  [:div {:data-role "fieldcontain" }  
     [:input  (merge {:type "checkbox" 
                      :name cbname
                      :id cbname
                      :class "custom"
                      }
                     (if (:select-by-default orig) 
                         {:checked "checked" }
                         {})) ]
     [:label {:for cbname } (:name orig) ] ]) 

(defn- required-cb
  "Returns the required checkbox.
  which is a disabled checkbox."
  [orig cbname]

  [:div {:data-role "fieldcontain" } 
     [:input {:type "checkbox" 
              :name cbname 
              :id cbname 
              :checked "checked"
              :class "custom"
              :disabled "disabled" 
              } ]
     [:label {:for cbname } (:name orig) ]])

(defn- selected-cb
  [orig rr]
  (list (let [cbname (str "cb-" (:protocol-id rr)) ]
     (if (:required orig) 
       (required-cb orig cbname)
       (optional-cb orig cbname)))))

(defn print-one 
  [orig published-protocols n]

  (let [rr (get-control orig published-protocols)
        selname (str "lang-" (:protocol-id rr))  
        cbname (str "cb-" (:protocol-id rr))  
        langs (:languages rr)]
        [:li { :class (if (= (rem n 2) 1) "odd" "even") }
          [:div.protocol-col1 (selected-cb orig rr) ]
          [:div.protocol-col3 
              [:label {:for selname  :class "select" } ]
              [:select { :class "select" :name selname :id selname }
                 (for [l langs] 
                      [:option {:value (:name l) } (:name l) ] ) ] ] ]))


(defn- process-protocols
  "Generates protocols section.  
  Display the name and the language if there is more than one language.  
  Also collect meta-data items and store in the session"

  [protocols published-protocols]
  (session-put! :protocols protocols)
  (session-put! :published-protocols published-protocols)
  [:ul.select-protocol-list 
      (for [pi (map-indexed vector protocols)] 
         (list 
           (print-one (last pi) published-protocols (first pi) )))])

(defn view 
  "Form to select protocols." 
  [ctx]
  (helper/rpms2-page 
    (helper/post-form "/view/select/protocols"
      [:div 
       (process-protocols (dsa/get-protocols) (dsa/get-published-protocols) ) ]
      [:div.centered (helper/submit-button "select-protocols-form") ])
    :title (i18n :hdr-select-protocols)))

(defn- needed-protocol-ids
  "Returns the protocol ids that need to be filled out.  This is the required
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

(defn perform
  "Once the protocols are selected, identify required meta-data items
  and go to the meta-data page."

  [ctx]
  (let [needed (needed-protocol-ids (:body-params ctx))
        publist (session-get :published-protocols) 
        _ (debug "NNN publist " publist)
        ;; select those items whose id is in needed
        ;; this is not the correct expression
        ;; for each item in publist, if the protocol-id was in needed then collect it
        ;; (contains? needed (get-in % [:protocol :id]))
        required-published (map my-find-published needed)
        _ (debug "NNN required-publist " (count required-published) " " required-published)
        ]
    ;; get a list of selected protocol ids then select those from the published protocols
    ;; save those in the session
    ;; then get the meta data items for all those, and then remove duplicates
    ;; put this into the session
    ;; get selected protocols
    ;; then get the meta data for each one
    (session-put! :required-published required-published)
    (session-put! :current-step "Form-1-Page-1")
    (debug "NNN number of published protocols "  (count (session-get :published-protocols))) 
    (debug "NNN Protocols to be filled out " (pr-str needed) )

   (flash-put! :header (str "Protocols to be filled out " (pr-str needed) )) 
   (helper/myredirect "/view/meta-data"))) 

(debug! perform)
