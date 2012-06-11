(ns ^{:doc "Collect consents - collects information from forms." }
  org.healthsciencessc.rpms2.consent-collector.collect-consents
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.mock :as mock]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! destroy-session! ]])
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


(defn- widget-not-implemented 
  [c m]
  (debug "widget not implemented")
  [:div.control "Widget Not Implemented " ]
)

(defn review-endorsement
  [c m]
  (debug "review-endortment not implemented")
  [:div.control "Review Endorsement" ]
)

(defn review-metaitem
  [c m]
  (debug "review-metaitemendortment not implemented")
  [:div.control "Review Meta Item" ]
)


(defn review-policy 
  [c m]
  (debug "review-policy metaitemendortment not implemented")
  [:div.control "Review Policy" ]
)


(defn media 
  [c m]
  (debug "media not implemented")
  [:div.control "Media" ]
)
  
(defn signature
  "Emits data for the signature widget. A map with the widgets state is passed
   to use in rendering the widget."
  [c m]
  [:div.control.signature 
   (:name c)
  [:div.sigpad-control
      [:div.sigPad  ; sigPad must be on div which directly contains sigNav
      [:ul.sigNav 
         #_[:li.clearButton [:a {:href "#clear"} "Clear" ] ] ; use an onclick event here
         [:li [:input {:type "submit" 
                       :data-theme "a"
                       :data-role "button"
                       :data-inline "true"
                       :value (:clear-label c)
                       :name (str "signature-btn-" (:name c))
                      } ] ]
      ] 
      [:div {:class "sig sigWrapper" }
        [:div.typed ] 
          [:canvas {:class "pad" :width "198" :height "55" }  ]
          [:input {:type "hidden" :name "output" :class "output" }  ]
      ]]]
   ])

(defn- true-or-not-specified? 
  [v]
  (not (= v false))) 

(defn policy-text
  "A PolicyText widget generates title and paragraph from a specific Policy."  
  [c _]
  [:div.control.policy-text
   (list 
     (let [policy (dsa/get-policy (:policy c))]

     ;; Display title if :render-title is missing or true 
     ;; and policy has a title
     (list 
       (if (and (true-or-not-specified? (:render-title c))
       ;;(if (and (not (= (:render-title c) false)) 
                (:title policy))
           [:div [:h1.title (:title policy)]])

     ;; Display text if :render-text is missing or true and policy has text
       (if (and (not (= (:render-text c) false))
                (:text policy))
             (map (fn [tt] [:div.text tt ]) (:text policy)))

     (if (not (= (:render-media c) false)) 
       [:div.render-media "Render media controls here" ]) ))) ])

(defn- lookup-data
  [c]
  (get (session-get :model-data) (keyword (:name c)) ))

(defn- checked-if-lookup-data-matches
  [c v]
  (let [d (get (session-get :model-data) (keyword (:name c)) )]
    (if (= d v) {:checked "checked" }  {})))

(defn policy-choice-buttons
  "Creates two buttons that allow you to opt in or opt out of one or more
  policies. A map with the widget's state is passed.
  Set the state of the selected radio button if one is selected in m"
  [c m]
  [:div.control.policy-choice-buttons 
    (dbg (str "policy-choice-buttons m=" (pprint-str m)
              " lookup data [" (lookup-data c) "]"))

    (helper/radio-btn-group {:btnlist (list (:true-label c) (:false-label c)) 
                            :group-name (:name c)
                            :selected-btn (lookup-data c)
                            })])

(defn data-change
  "Displays meta-data item and a flag if it has been selected for change."
  [c m]
  [:div.control.data-change
   (list 
     (dbg [:div.debug "DATA CHANGE M IS " (pprint-str m) ])
     (for [t (:meta-items c)] 
           (list
             (let [md (dsa/get-metadata t)
                   data-name (str "meta-data-btn-" (:mdid md))
                   model-data (session-get :model-data)
                   data-value ( (keyword data-name) model-data ) ]
                  [:div.ui-grid-b
                    [:div.ui-block-a.metadata (:label md) ]
                    [:div.ui-block-b.metadata (:value md) (if data-value [:span.changed data-value])]  
                          ;; note should use the value
                          ;; they entered previously 
                    [:div.ui-block-c 
                          [:input { :type "submit" 
                                    :data-theme "a"
                                    :data-role "button"
                                    :data-inline "true"
                                    :value "Change"
                                    :name data-name 
                                   } ]
                    ]
                   ]))))
  ])

(defn policy-button
  [c _]
  [:div.control 
  [:input { :type "submit" 
            :data-theme "d"
            :data-role "button"
            ;; :data-inline "true"
            :name (str "action-btn-" (:label c))
            :value (:label c)
           } ]
  ])

(defn text
  "A Text widget generates a title and paragraph representations for 
  text values set on the control. The control requires that either the 
  title, the text, or both be set."
  [c _]
  [:div.control.text
   (if (:title c) [:h1.title (:title c) ])
   (list (for [t (:text c)] [:p t ])) ])

(defn policy-checkbox
  "Display the checkbox.  Remember the state of the checkbox."
  [c m]
  [:div.control 
    (dbg (str "policy-checkbox " (pprint-str m)
              " lookup data [" (lookup-data c) "]"))
    (helper/checkbox-group {:name (:name c) :label (:label c) :value (lookup-data c) }) ])

(defn- process-control
  "Displays the widget c by invoking the method with the widget's type.
  This method takes the control (c) and the model data associated with that 
  control."
  [c]

  (list 
    (let [ns "org.healthsciencessc.rpms2.consent-collector.collect-consents/"
          nm (str ns (:type c))
          func  (resolve (symbol nm)) 
          wmodel (helper/lookup-widget-by-name (:name c)) ]
          (list
             (if func 
                 [:div [:div c ] (func c wmodel) ]
                 [:div "Unrecognized control " [:span.control-type  (:type c) ] c ])
             (dbg [:div.debug [:span.standout (:name c) ] " WM " wmodel " " [:pre (pprint-str c)]])))))

(defn- process-section
  "Display section in a div."
  [s]
  [:div.section (map process-control (:contains s)) ])

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
              [:div "Data " (pprint-str (session-get :model-data))] ])
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
  [ctx]
  (let [s (session-get :collect-consent-status)]
    (helper/rpms2-page 
       (helper/collect-consent-form "/collect/consents"
           [:div.centered 
              [:div.finished1 "Thank You!" ]
              [:div.finished2 (str "Your selected " (helper/org-protocol-label) "s are complete.") ]
              [:div.finished3 "Return the device to the clerk." ] ]
           (helper/standard-submit-button {:value "Continue" :name "next" })) 
       :title "Consents Complete" )))

(defn- update-session
  "Merges the map, logs the new map, saves in session, and returns merged map."
  [m]
  (let [s (session-get :collect-consent-status)
        new-map (merge s m)]
       (debug "update-session: " (pprint-str new-map))
       (session-put! :collect-consent-status new-map)
       new-map))


(defn- check-for-special
  [w]
  ;; find the named item in the list
  (debug "check-for-special " w)
)

(defn- capture-data
  [ctx]

  ;; find the type of each data item and apply post-processing if necessary
  ;; remove :next
  (let [parms (:body-params ctx)
        orig (session-get :model-data)
        valid-keys (session-get :valid-keys)
        stuff (map check-for-special (dissoc parms :next :previous))
        n (dissoc (merge orig parms) :next :previous) ]
        (session-put! :model-data n)
        (debug "capture-data " n " orig " orig " ctx " ctx )))

(defn- has-another-form?
  "this test will change"
  [s]
  (helper/get-nth-form (inc (:current-form-number s)) ))

(defn- finish-form
  []
  (debug "1 save data in last form and start new form: " (session-get :model-data))
  (debug "2 save data in last form and start new form: " (pprint-str (session-get :model-data)))
)

(defn- advance-to-next-form
  "Advances to the next form.  Returns nil if no such form exists.
  If there's a current form, then should close that out before 
  continuing on to the next one.
  Sets page to the first page specified by 'which-flow' 
  Increments the form number."
  []

  (let [s (session-get :collect-consent-status)
        which-flow (:which-flow s) ]
    (if-let [next-form (helper/get-nth-form (inc (:current-form-number s)) )]
       (let [form (:form next-form)
             n (:current-form-number s)]
            (do
              (helper/finish-form)
              (debug "KKK advance-to-next-form "  (which-flow form) " n "  n " " 
                            (get-named-page form (which-flow form)))
              (debug "KKK advance-to-next-form  FORM " (form-title form) " " form)
            ;; would be getting the nth form  instead of passing the form
            (let [p (get-named-page form (which-flow form))
                 modified-state {:form form
                                 :state :begin 
                                 :page p
                                 :page-name (:name p)
                                 :current-form-number (inc n) 
                                }]
                 (update-session modified-state)
             )))
            nil)))

(defn- navigation-buttons
  "Displays the navigation button for the page, which will be
  a Continue button and optionally a previous button."
  [s]

  [:div 
   (if (:previous (:page s))
       (helper/standard-submit-button {:value "Previous" :name "previous" }))
   (helper/standard-submit-button {:value "Continue" :name "next" }) ]) 

(defn- get-types
  [col]
  (set (flatten (list (for [b col] (:type b))))))

(defn- has-signature?
  [p]
  ;; a page contains sections, sections contain widgets
  (let [section-list (:contains p) 
        ;;_ (debug "has-signature? section-list is " (pprint-str section-list))
        ww (map (fn [n] (:contains n)) section-list)
        ;;_ (debug "has-signature? ww is " (pprint-str ww))
        bb (map :type (flatten ww))
        ;;_ (debug "has-signature? bb is " (pprint-str bb))
        has-sig (contains? (set bb) "signature")
        _ (debug "has-signature? RETURNING " (pprint-str has-sig)) ]
    has-sig))

(defn view 
  "Collect and review consents processes. Displays current page."
  [ctx]

  ;; first time here, initialize 
  (if-let [s (session-get :collect-consent-status)]
    (debug "Already initialized " (pprint-str (:name (:page s))))
    (helper/init-consents))

  (let [s (session-get :collect-consent-status)]
    (helper/rpms2-page 
       (helper/collect-consent-form "/collect/consents"
           (display-page (:page s) s) 
           (navigation-buttons s)) 
       :title (form-title (:form s)) )))

(defn- update-session
  "Merges m into the session's :collect-consent-status map, 
  Logs the new map, saves in session, and returns merged map."
  [m]
  (let [s (session-get :collect-consent-status)
        new-map (merge s m)]
       (debug "update-session: " (pprint-str new-map))
       (session-put! :collect-consent-status new-map)
       new-map))

(defn- goto-page
  [page]

  (debug "Going to page: " (:name page))
  (update-session {:page page :page-name (:name page) })
  (helper/myredirect "/collect/consents"))

(defn- get-matching-btns 
  "Get parameters with name starting with string 's'.
  Returns a list, which will be empty if there are no matches."
  [parms s]
  (filter #(.startsWith (str (name %)) s) (keys parms)))

(defn- has-any?
  "Are there any parameters starting with the string 's'?"
  [parms s]

  (> (count (get-matching-btns parms s)) 0))

(defn perform
  "Collect consents. "

  [{parms :body-params :as ctx}]

  (debug "288 perform " ctx)

  (let [s (session-get :collect-consent-status)
        form (:form s) 
        nxt (if-let [nxt (:next (:page s))] (get-named-page form nxt) nil)]

    (capture-data ctx)

    ;; see if previous or continue was pressed
    ;; either go to the next page or show end of collection page
    (cond 
      (has-any? parms "action-btn-")
      (helper/myredirect (str "[action button "  (pprint-str (get-matching-btns parms "action-btn-")) "]")
                         "/collect/consents")

      (has-any? parms "signature-btn-")
      (helper/myredirect (str "[signature button "  (pprint-str (get-matching-btns parms "signature-btn-")) "]")
           "/collect/consents")

      (has-any? parms "meta-data-btn-")
      (helper/myredirect (str "[meta-data button " (pprint-str (get-matching-btns parms "meta-data-btn-")) "]")
             "/collect/consents")

      (contains? parms :previous)
      ;; if previous button pressed and prev page available
      (if-let [pg-name (:previous (:page s)) ]
              (goto-page (get-named-page form pg-name))
              ;; else, maybe go to previous form here
              (helper/flash-and-redirect 
                "Previous pressed, no previous page available." 
                "/collect/consents"))

      ;; if next page available
      nxt 
      (goto-page nxt)

      ;; At end of current form, set current page to start of next form,
      ;; or if there are none, set current page to start of review 
      (advance-to-next-form)
      (helper/myredirect "Advancing to next form" "/collect/consents")

     (:review-confirmed s)
          (helper/myredirect "/view/unlock")

     :else
          (do (update-session {:page (get-named-page form (:summary-start form))
                               :page-name (:summary-start form)
                               :review-confirmed :true } )
             (view-finished ctx)))))

