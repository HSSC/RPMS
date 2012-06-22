(ns ^{:doc "Collect consents - collects information from forms." }
  org.healthsciencessc.rpms2.consent-collector.collect-consents
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.mock :as mock]
   [org.healthsciencessc.rpms2.consent-collector.formutil :as formutil]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info warn error)])
  (:use [clojure.pprint :only (pprint)])
  (:use [org.healthsciencessc.rpms2.consent-collector.debug :only [debug! pprint-str]])
  (:use [org.healthsciencessc.rpms2.consent-collector.config :only [config]])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

(defn- dbg
  "Displays m only if verbose debugging is enabled."
  [m]

  (if-let [b (config "verbose-collect-consents")]
    [:div.debug m ]))

(defn unimplemented-widget
  "Displays an unrecognized or unimplemented widget."

  [{:keys [widget value form review] :as m}]
  [:div [:span.standout "Unrecognized control " 
         "TYPE  " [:span.standout "TYPE " (:type widget) ] 
         "WIDGET " [:span.standout "WIDGET " widget ] 
         [:pre (pprint-str m) ]] 
   [:span.control-type  (:type widget) ] widget ])

(defn- control
  "Displays widget by invoking the method with the widget's type.
  A map is passed in which contains the widget, the current value of the widget,
  the form and flag indicated whether this is in the review phase (so the widget
  can render itself appropriately)."

  [{:keys [widget value data-model] :as m}]

  (list 
    (let [ns "org.healthsciencessc.rpms2.consent-collector.collect-consents/"
          func  (if-let [f (resolve (symbol (str ns (:type widget))))] f unimplemented-widget)
          wdata (helper/data-for widget data-model) ]
          [:div 
             (func (merge m {:value wdata } )) 
             (dbg [:div.debug [:span.standout (:name widget) ] [:span.data wdata ] widget ]) ])))
       
(defn review-endorsement
  "A ReviewEndorsement widget is used to review endorsements 
  collected during consent process."

  [{:keys [widget value] :as m}]
  [:div.control.review-endorsement 
    [:div.ui-grid-b
       [:div.ui-block-a.metadata 
        (if-let [s (:endorsement-label widget)] 
          s 
          (if-let [t (:title widget)] t "Endorsement-label"))  
            (helper/signaturePadDiv (:name widget) value ) ]
        [:div.ui-block-c.metadata 
         (helper/submit-btn {:value (:label widget) 
                             :name (str "review-edit-btn-" (:returnpage widget)) }) ]]])

(defn review-metaitem
  "Display meta item. The widget's meta-item id is the key into
  the current forms meta-data map."

  [{:keys [widget] :as m}]
  [:div.control.review 
     (let [mi (:meta-items (helper/current-form))
           mitem ((keyword (:meta-item widget)) mi)
           data-name (str helper/META_DATA_BTN_PREFIX (:meta-item widget))
           model-data (session-get :model-data)
           data-value ( (keyword data-name) model-data ) ]
    [:div.ui-grid-b
      [:div.ui-block-a.metadata (:label mitem) ]
      [:div.ui-block-b.metadata 
          [:span {:class (if data-value "changed" "") } (:value mitem) ]]
       [:div.ui-block-c.metadata 
          (helper/submit-btn {:value (:label widget) 
                              :name (str "review-meta-edit-btn-" (:meta-item widget)) }) ]] )])

(defn find-policy 
  [p]
  (let [policies (:policies (helper/current-form))]
       ((keyword p) policies)))

(defn review-policy 
  "A ReviewPolicy widget provides a controller that allows the collector to 
  review consents collected for policies during the review process.
  The value will be the value associated with the named widget.
  choicebuttons, policybutton checkbox-label

  Find the widget associated with the policy and use that value."
  [{:keys [widget] :as m}]
  [:div.control.review 
     (list 
       (let [policy (find-policy (:policy widget))]
           [:div.ui-grid-b
                [:div.ui-block-a.metadata (:title policy) ]  
                [:div.ui-block-b.metadata (:title widget) ]
                [:div.ui-block-c.metadata 
                 (helper/submit-btn {:value (:label widget) 
                                     :name (str "review-edit-btn-" (:returnpage widget)) })]])) ])

(defn media 
  [c m]
  [:div.control "Media" ])

(defn signature
  "Emits data for signature widget. A map with widgets state is passed
   to use in rendering the widget."
  [{:keys [widget value] :as m}]

  [:div.control.signature 
   (:name widget)
    [:div.ui-grid-b
       [:div.ui-block-a "" ]
       [:div.ui-block-b "" ]
       [:div.ui-block-c.right 
          [:a {:href "#"
               :data-role "button" 
               :data-theme "a" 
               :onclick "$('.sigPad').signaturePad().clearCanvas()"} 
         (:clear-label widget)]]]
   (helper/signaturePadDiv (:name widget) value) ])

(defn- true-or-not-specified? 
  [v]
  (not (= v false))) 

(defn policy-text
  "A PolicyText widget generates title and paragraph from a specific Policy."  
  [{:keys [widget value form review] :as m}]
  [:div.control.policy-text
   (list 
     (let [policy (find-policy (:policy widget))]

     ;; Display title if :render-title is missing or true 
     ;; and policy has a title
     (list 
       (if (and (true-or-not-specified? (:render-title widget))
       ;;(if (and (not (= (:render-title widget) false)) 
                (:title policy))
           [:div [:h1.title (:title policy)]])

     ;; Display text if :render-text is missing or true and policy has text
       (if (and (not (= (:render-text widget) false))
                (:text policy))
             (map (fn [tt] [:div.text tt ]) (:text policy)))

     (if (not (= (:render-media widget) false)) 
       [:div.render-media "Render media controls here" ]) ))) ])

(defn policy-choice-buttons
  "Creates two buttons that allow you to opt in or opt out of one or more
  policies. The widget's state is passed in to set current selection."
  [{:keys [widget value] :as m}]
  [:div.control.policy-choice-buttons 
    (helper/radio-btn-group {:btnlist (list (:true-label widget) (:false-label widget)) 
                             :group-name (:name widget)
                             :selected-btn value
                            })])

(defn data-change
  "Displays meta-data item and a flag if it has been selected for change."
  [{:keys [widget value form review] :as m}]
  [:div.control.data-change
   (list 
     (dbg [:div.debug "DATA CHANGE IS " (pprint-str value) ])
     (for [nm (:meta-items widget)] 
           (list
             (let [md (dsa/get-metadata nm)
                   data-name (str helper/META_DATA_BTN_PREFIX (:mdid md))
                   model-data (session-get :model-data)
                   data-value ( (keyword data-name) model-data ) 

                   mi (:meta-items (helper/current-form))
                   mitem ((keyword nm) mi)
                   v (:value mitem) ]

                  [:div.ui-grid-b
                    [:div.ui-block-a.metadata (:label mitem) ] 
                    [:div.ui-block-b.metadata 
                       [:span {:class (if data-value "changed" "") 
                               :id (:mdid md) } v ]]  

                     [:input {:type "hidden" 
                              :id (str "hidden-" (:mdid md)) 
                              :name data-name 
                              :value "NO"
                              }]
                    [:div.ui-block-c 
                     [:p [:a 
                          {:href "#popup" 
                           :data-rel "dialog" 
                           :onclick "org.healthsciencessc.rpms2.core.data_change_clicked(this)"
                           :data-change-class (pr-str (str (:value md)))
                           :mdid (pr-str (str (:mdid md)))
                           :data-role "button" 
                           :data-theme "a" } "Change" ] ]
                     ] ])))) ])

(defn policy-button
  "Displays the policy button. Once the button has been pushed,
  the data in the model is set and the style is changed." 

  [{:keys [widget value] :as m}]
  [:div.control.policy-button 
   (helper/submit-btn {:data-theme (if value "b" "d" )
                       :data-inline "false"
                       :name (str helper/ACTION_BTN_PREFIX (:name widget))
                       :value (:label widget) }) ])

(defn text
  "A Text widget generates a title and paragraph representations for 
  text values set on the control. The control requires that either the 
  title, the text, or both be set."
  [{:keys [widget] :as m}]
  [:div.control.text
   (if (:title widget) [:h1.title (:title widget) ])
   (list (for [t (:text widget)] [:p t ])) ])

(defn policy-checkbox
  "Displays checkbox, using the remembered state.  
  A hidden input is created so we can clear the checkbox from the data
  model if it is not checked and the form is submitted.
  Checkboxes are included in form submission parameters if they are not checked."
  [{:keys [widget value form] :as m}]

  [:div.control 
    [:input {:type "hidden" 
             :name (str helper/CHECKBOX_BTN_PREFIX (:name widget)) } ]
    (helper/checkbox-group {:name (:name widget) 
                            :label (:label widget) 
                            :value value  }) ])

(defn- section
  "Creates section div containing all widgets in this section."
  [s dm]
  [:div.section (map #(control {:widget % 
                                :form (helper/current-form) 
                                :data-model dm 
                                }) (:contains s)) ])


(defn- page-dbg
  [p s]

  (dbg [:div.debug
       [:div.left"Page "  [:span.standout (:name (:page s)) ] " " (:title p)    
       " Form #" [:span.standout (inc (:current-form-number s))] " of " 
       [:span.standout (count (session-get :protocols-to-be-filled-out)) ] ]
       [:div "Data  " (helper/pr-model-data) ] ]))

(defn- display-page
  "Displays sections. Checks for missing page
  and optionally displays debugging information.

  If page is available, displays each section of the page
  in a separate div." 

  [p s dm]

  (if (= nil p) 
    [:h1 "Unable to show page - missing page " 
         (if-let [pn (:page-name s) ]
             [:span.standout pn ]) ])
    [:div (page-dbg p s)
      (if (helper/in-review?) [:h1 "Summary" ] )
      [:div (map #(section % dm) (:contains p)) ]])

(defn- form-title
  [f]
  (get-in f [:header :title]))

(defn- get-named-page
  "Find page named 'n' in form 'f'"
  [f n]
  (first (filter #(= (:name %) n ) (:contains f) )))

(defn- view-update-information
  [ctx nm]

  (let [s (session-get :collect-consent-status)
        mi (:meta-items (helper/current-form))
        mitem ((keyword nm) mi)
        l (:label mitem)
        v (:value mitem) ]
      (helper/rpms2-page 
       [:div.collect-consent-form
          [:h2 "Update the following information: " nm]
          (dbg [:div.debug (session-get :model-data) ])
          [:form {:action (helper/mypath "/collect/consents") 
                  :method "GET" 
                  :data-ajax "false" 
                  :data-theme "a" } 
            [:div.ui-grid-b
               [:div.ui-block-a  l ]
               [:div.ui-block-b   
                     [:input {:name nm 
                              :value v } ] ] ]
          [:div.submit-area (helper/submit-btn {:value "Update" :name "meta-data-update-btn-next" }) ]]] 
       :title "Update Information" )))


(defn- view-finished
  "All forms have been processed.  If we just finished collecting the consents,
  display a Thank You page. Otherwise, go to the collect witness."
  [ctx]

  (let [s (session-get :collect-consent-status)]
    (if (= (:which-flow s) helper/COLLECT_START_PAGE)
      (helper/rpms2-page 
       [:div.collect-consent-form
          [:form {:action (helper/mypath "/view/unlock") 
                  :method "GET" 
                  :data-ajax "false" 
                  :data-theme "a" } 
           [:div.centered 
            (dbg [:div.debug (helper/print-all-form-data) ] )
              [:div.finished1 "Thank You!" ]
              [:div.finished2 (str "Your selected " (helper/org-protocol-label) "s are complete.") ]
              [:div.finished3 "Return the device to the clerk." ] ]
          [:div.submit-area (helper/submit-btn {:value "Continue" :name "next" }) ]]] 
       :title "Consents Complete" )
      
      (helper/myredirect "/witness/consents"))))

(defn- check-for-special
  [w]
  ;; find the named item in the list
  (debug "check-for-special " w))

(defn- has-another-form?
  "this test will change"
  [s]
  (helper/get-nth-form (inc (:current-form-number s)) ))

(defn- navigation-buttons
  "Displays the navigation button for the page, which will be
  a Continue button and optionally a previous button.
  Don't display the previous button if there's a pending return page."
  [s]

  [:div 
   (if (and (:previous (:page s))
            (not (helper/get-return-page)))
       (helper/submit-btn {:value "Previous" :name "previous" }))
   (helper/submit-btn {:value "Continue" :name "next" }) ]) 

(defn- emit-page
  [s data-model]

  (helper/rpms2-page 
     (helper/collect-consent-form "/collect/consents"
         (display-page (:page s) s data-model) 
         (navigation-buttons s)) 
       :title (form-title (:form s)) 
       :second-page "placeholder" ))

(defn view 
  "Collect and review consents processes. Displays current page."
  ([] (view {}))
  ([ctx]

  ;; first time here, initialize 
  (if-let [s (session-get :collect-consent-status)]
    (debug "Already initialized: Page name: " (pprint-str (:name (:page s))))
    (helper/init-consents))

  (let [s (session-get :collect-consent-status)]
    (emit-page s (session-get :model-data)))))

(defn- get-matching-btns 
  "Get parameters with name starting with string 's'.
  Returns a list, which will be empty if there are no matches."
  [parms s]
  (filter #(.startsWith (str (name %)) s) (keys parms)))

(defn- find-review-edit-page
  [parms]
  (helper/find-special-page parms "review-edit-btn-"))

(defn- find-review-meta-edit-page
  [parms]
  (helper/find-special-page parms "review-meta-edit-btn-"))

(defn- has-any?
  "Are there any parameters starting with the string 's'?"
  [parms s]
  (> (count (get-matching-btns parms s)) 0))

(defn get-next-page
  "Will need to do something different if in review Eg. If we have
  gone back to edit something. "
  [s]
  (if-let [nxt (:next (:page s))]
    (get-named-page (:form s) nxt) 
    nil))

(defn perform
  "Collect consents."

  [{parms :body-params :as ctx}]

  (debug "288 perform " ctx)
  (let [s (session-get :collect-consent-status)
        form (:form s) 
        nxt  (get-next-page s) 
        data-model (helper/save-captured-data parms) ]

    (cond 
      ; If there's a return page, go there
      ; if meta-data has been updated, need to send that to the server
      ; otherwise may also need to send the updated data to the server
      (helper/get-return-page)  
      (let [pg-name (helper/get-return-page)]
         (do 
           (debug "GOING TO RETURN PAGE " pg-name)
           (helper/clear-return-page)
           (helper/set-page (get-named-page form pg-name))
           (view)))

      ; If the user wants to edit a reviewed item
      (find-review-edit-page parms)
      (let [pg-name (find-review-edit-page parms)]
            (do 
              (debug "GOING TO REVIEW EDIT PAGE: [" pg-name "]")
              (helper/save-return-page)
              (helper/set-page (get-named-page form pg-name))
              (view)))

      ; If the user wants to edit a meta data item
      (find-review-meta-edit-page parms)
      (let [pg-name (find-review-meta-edit-page parms)]
            (do 
              (debug "GOING meta-EDIT PAGE: [" pg-name "]")
              (view-update-information ctx pg-name)))

      ;; special buttons which are completely processed by save-captured-data
      (has-any? parms helper/ACTION_BTN_PREFIX )
      (view)

      (has-any? parms "signature-btn-")
      (view)

      (contains? parms :previous)
      (do (if-let [pg-name (:previous (:page s)) ]
             (helper/set-page (get-named-page form pg-name)))
          (view))

      ;; if next page available
      nxt 
      (do (helper/set-page nxt)
          (view))

      (helper/finish-form)
      (view)

     :else
     (view-finished ctx))))

