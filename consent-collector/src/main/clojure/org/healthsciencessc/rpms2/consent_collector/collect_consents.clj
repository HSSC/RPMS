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
  (:use [clojure.data.json :only (json-str)])
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

(defn- process-control
  "Displays the widget 'widget' by invoking the method with the widget's type.
  A map is passed in which contains the widget, the current value of the widget,
  the form and flag indicated whether this is in the review phase (so the widget
  can render itself appropriately)."

  [{:keys [widget value form review] :as m}]

  (list 
    (let [ns "org.healthsciencessc.rpms2.consent-collector.collect-consents/"
          func  (if-let [f (resolve (symbol (str ns (:type widget))))] f unimplemented-widget)
          wdata (helper/data-for widget) ]
          [:div 
             (func (merge m {:value wdata} )) 
             (dbg [:div.debug [:span.standout (:name widget) ] [:span.data wdata ] widget ]) ])))
       
(defn- sigpad
  "Emits signature pad control.  
  The signature is processed by it's own form and part of the standard
  collect consents form. "
  []

  [:div.sigpad-control#signature-pad-item
  [:form.sigPad {:method "POST" :data-ajax "false" }
      #_[:label {:for "name"} "Print your name" ]
      #_[:input {:type "text" :name "name" :id "name" :class "name" } "Print your name" ]
      #_[:p.typeItDesc "Review your signature" ]
      [:p.drawItDesc "Draw your signature" ]
   [:ul.sigNav
    #_[:li.typeIt [:a {:href "#type-it"} "Type" ] ]
    #_[:li.drawIt [:a {:href "#draw-it"} "Draw It" ] ]
    [:li.clearButton [:a {:href "#clear"} "Clear" ] ]
    ]
     [:div {:class "sig sigWrapper ccsig" }
      [:div.typed ] 
       [:canvas {:class "pad" :width "198" :height "55" }  ]
       [:input {:type "hidden" :name "output" :class "output" }  ]
     ]
   ]])

(defn review-endorsement
  "A ReviewEndorsement widget allows the collector to review endorsements that were 
  collected during the consent process."

  [{:keys [widget value form review] :as m}]
  [:div.control.review-endorsement 
    (dbg [:span "review-endorsement Signature Named " (:name widget) " current value is " (pprint-str value) ])
    [:div.ui-grid-b
        [:div.ui-block-a.metadata (if-let [s (:endorsement-label widget)] 
                                          s 
                                          (if-let [t (:title widget)] t "Endorsement-label"))  ]
        [:div.ui-block-c.metadata (helper/submit-btn {:value "Edit" 
                                                      :name (str "review-edit-btn-" (:returnpage widget)) }) ]]])
(defn review-metaitem
  [{:keys [widget value form review] :as m}]
  [:div.control.review 
    (dbg [:span "Review Meta Item Named " (:name widget) " current value is " (pprint-str val) ])
    [:div.ui-grid-b
        [:div.ui-block-a.metadata "Meta Item Label" (:metaitem-label widget) ]
        [:div.ui-block-b.metadata "Meta Item Value" (:metaitem-value widget) ]
        [:div.ui-block-c.metadata (helper/submit-btn {:value "Edit" 
                                                      :name (str "review-edit-btn-" (:returnpage widget)) }) ]]])

(defn review-policy 
  "A ReviewPolicy widget provides a controller that allows the collector to 
  review consents collected for policies during the review process.
  The value will be the value associated with the named widget.
  choicebuttons, policybutton checkbox-label"
  [{:keys [widget value form review] :as m}]
  [:div.control.review 
     (list 
       (let [policy (dsa/get-policy (:policy widget)) ]
             [:div.ui-grid-b
                [:div.ui-block-a.metadata (:title widget) ]  
                [:div.ui-block-b.metadata "Label" ]
                [:div.ui-block-c.metadata 
                 (helper/submit-btn {:value (:label widget) 
                                     :name (str "review-edit-btn-" (:returnpage widget)) })]])) ])
(defn media 
  [c m]
  [:div.control "Media" ])

(defn signature
  "Emits data for the signature widget. A map with the widgets state is passed
   to use in rendering the widget."
  [{:keys [widget value form review] :as m}]

  ;;(debug "SIGNATURE: widget " widget " VALUE " (pprint-str value))
  ;;(debug "SIGNATURE: widget " widget " jSON VALUE " (json-str value) )

 #_(if value (do
              (debug "adding signature with a value ")
              (let [jscript (str 
"$(document).ready(function() { $('.sigPad').signaturePad().regenerate(" (json-str value) "); }")]
                (debug "\n\njscript is " jscript "\n\n")
                (session-put! :jscript 
                              (str "\n<!-- ZZZZ HEY --><script> " jscript "</script>") ))))

  [:div.control.signature 
   (:name widget)
  [:div.sigpad-control
      [:div.sigPad  ; sigPad must be on div which directly contains sigNav
       ;; [:script "var signature-api-" (:name widget) "= \"" (json-str value) "\";" ]
      [:ul.sigNav 
         #_[:li.clearButton [:a {:href "#clear"} "Clear" ] ] ; use an onclick event here
         [:li (helper/submit-btn { :value (:clear-label widget)
                                   :name (str "signature-btn-" (:name widget)) }) ] 
      ] 
      [:div {:class "sig sigWrapper" }
        [:div.typed ] 
          [:canvas {:class "pad" :width "198" :height "55" }  ]
          [:input {:type "hidden" 
                   :name (:name widget) 
                   :class "output" 
                   :value value } ]
      ]]]
   ])

(defn- true-or-not-specified? 
  [v]
  (not (= v false))) 

(defn policy-text
  "A PolicyText widget generates title and paragraph from a specific Policy."  
  [{:keys [widget value form review] :as m}]
  [:div.control.policy-text
   (list 
     (let [policy (dsa/get-policy (:policy widget))]

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
  [{:keys [widget value form review] :as m}]
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
     (for [t (:meta-items widget)] 
           (list
             (let [md (dsa/get-metadata t)
                   data-name (str helper/META_DATA_BTN_PREFIX (:mdid md))
                   model-data (session-get :model-data)
                   data-value ( (keyword data-name) model-data ) ]
                  [:div.ui-grid-b
                    [:div.ui-block-a.metadata (:label md) ]
                    [:div.ui-block-b.metadata 
                     [:span {:class (if data-value "changed" "") } (:value md) ]
                     (if data-value [:span
                                     "This item has been flagged for change
                                     and may be edited during the review process." ]) ]  
                    [:div.ui-block-c (helper/submit-btn {:value "Change" 
                                                         :name data-name } )] ])))) ])

(defn policy-button
  "Displays the policy button." 

  [{:keys [widget value form review] :as m}]
  [:div.control.policy-button 
   (dbg (if review 
       [:div "in review values is " value [:div "the label is " (:label widget) ]] ))
   (helper/submit-btn {:data-theme (if value "e" "d" )
                       :data-inline "false"
                       :name (str helper/ACTION_BTN_PREFIX (:name widget))
                       :value (:label widget)
                      }) ])

(defn text
  "A Text widget generates a title and paragraph representations for 
  text values set on the control. The control requires that either the 
  title, the text, or both be set."
  [{:keys [widget value form review] :as m}]
  [:div.control.text
   (if (:title widget) [:h1.title (:title widget) ])
   (list (for [t (:text widget)] [:p t ])) ])

(defn policy-checkbox
  "Display the checkbox.  Remember the state of the checkbox."
  [{:keys [widget value form review] :as m}]
  [:div.control 
    (helper/checkbox-group {:name (:name widget) :label (:label widget) :value value }) ])


(defn- process-section
  "Display section in a div. 
  Displays all the widgets in the section"
  [s]
  [:div.section (map (fn [n] (process-control {:widget n :form (helper/current-form) })) (:contains s)) ])

(defn- display-page
  "Displays sections. Checks for missing page
  and optionally displays debugging information.

  If page is available, displays each section of the page
  in a separate div." 

  [p s]

  (if (= nil p) 
    [:h1 "Unable to show page - missing page " 
         (if-let [pn (:page-name s) ]
             [:span.standout pn ]) ])
    [:div
       (dbg [:div.debug
              [:div.left"Page "  [:span.standout (:name (:page s)) ] " " (:title p)    
                " Form #" [:span.standout (inc (:current-form-number s))] " of " 
               [:span.standout (count (session-get :protocols-to-be-filled-out)) ] ]
              [:div "Data  " (helper/pr-model-data) ] ])
       [:div (map process-section (:contains p)) ]
     ])

(defn- form-title
  [f]
  (get-in f [:header :title]))

(defn- get-named-page
  "Find page named 'n' in form 'f'"
  [f n]
  (first (filter #(= (:name %) n ) (:contains f) )))

(defn- view-finished
  "All the forms have been processed.  If we just finished collecting the consents,
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
  a Continue button and optionally a previous button."
  [s]

  [:div 
   (if (:previous (:page s))
       (helper/submit-btn {:value "Previous" :name "previous" }))
   (helper/submit-btn {:value "Continue" :name "next" }) ]) 

(defn view 
  "Collect and review consents processes. Displays current page."
  [ctx]

  ;; first time here, initialize 
  (if-let [s (session-get :collect-consent-status)]
    (debug "Already initialized: Page name: " (pprint-str (:name (:page s))))
    (helper/init-consents))

  (let [s (session-get :collect-consent-status)]
    (helper/rpms2-page 
       (helper/collect-consent-form "/collect/consents"
           (display-page (:page s) s) 
           (navigation-buttons s)) 
       :title (form-title (:form s)) 

;;       :end-of-page-stuff (if-let [ep (session-get :jscript)] 
 ;;                           (str "\n\n\n<script>" ep "</script>")
  ;;                          (str "\n\n<!-- END OF PAGE STUFF -->"))

      )))

(defn- get-matching-btns 
  "Get parameters with name starting with string 's'.
  Returns a list, which will be empty if there are no matches."
  [parms s]
  (filter #(.startsWith (str (name %)) s) (keys parms)))



(defn- find-special-page
  [parms str1 ]
  (let [s str1
        len (count s)
        pg-name (map (fn [n] (.substring (name n) len)) (get-matching-btns parms str1))]
        (first pg-name) ))

(defn- find-review-edit-page
  [parms]
  (find-special-page "review-edit-btn-"))

(defn- find-meta-item-page
  [parms]
  (find-special-page helper/META_DATA_BTN_PREFIX ))

(defn- find-review-edit-or-meta-item-page-name
  [parms]
  (or (find-review-edit-page parms) 
       (find-meta-item-page parms)))


(defn- goto-special-page
  [pg-nm]

  (let [page (get-named-page (helper/current-form) pg-nm) ]
       (do (helper/save-return-page)
           (if page (helper/set-page page)
                    (flash-put! :header "Unable to find edit page"))
           (helper/myredirect "/collect/consents"))))


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
        nxt  (get-next-page s) ]

    (helper/save-captured-data parms) 

    ;; see if previous or continue was pressed
    ;; either go to next page or show end of collection page
    (cond 
      (helper/get-return-page)
      (do
        (let [pg-name (helper/get-return-page)]
              (helper/clear-return-page)
              (helper/set-page (get-named-page form pg-name))
              (helper/myredirect "/collect/consents")))

      (has-any? parms helper/META_DATA_BTN_PREFIX )
      (helper/myredirect (str "[meta-data button " (pprint-str (get-matching-btns parms helper/META_DATA_BTN_PREFIX )) "]")
             "/collect/consents")

      ;; special buttons which are completely processed by save-captured-data
      (has-any? parms helper/ACTION_BTN_PREFIX )
      (helper/myredirect (str "[action button "  (pprint-str (get-matching-btns parms helper/ACTION_BTN_PREFIX )) "]")
                         "/collect/consents")

      (has-any? parms "signature-btn-")
      (helper/myredirect (str "[signature button "  (pprint-str (get-matching-btns parms "signature-btn-")) "]")
           "/collect/consents")


      (contains? parms :previous)
      (if-let [pg-name (:previous (:page s)) ]
              (do (helper/set-page (get-named-page form pg-name))
                  (helper/myredirect "/collect/consents"))
              (helper/myredirect "Previous pressed, no previous page available." 
                                 "/collect/consents"))

      ;; if next page available
      nxt 
      (do
        (helper/set-page nxt)
        (helper/myredirect "/collect/consents"))

      (helper/finish-form)
      (helper/myredirect "/collect/consents")

     :else
     (view-finished ctx))))
