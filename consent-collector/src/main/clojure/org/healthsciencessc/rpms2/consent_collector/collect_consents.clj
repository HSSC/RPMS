(ns org.healthsciencessc.rpms2.consent-collector.collect-consents
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! destroy-session! ]])
  (:use [clojure.tools.logging :only (debug info warn error)])
  (:use [clojure.pprint :only (pprint)])
  (:use [org.healthsciencessc.rpms2.consent-collector.debug :only [debug! pprint-str]])
  (:use [org.healthsciencessc.rpms2.consent-collector.config :only [config]])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

(def ^:const ^:private COLLECT_START_PAGE :collect-start)
(def ^:const ^:private REVIEW_START_PAGE :summary-start)

(defn signature
  "Emits data for the signature widget. A map with the widgets state is passed
   to use in rendering the widget."
  [c m]
  [:div.control.signature "Your signature is requested: " 
    [:div "Guarantor " (:name c)]
    #_[:div [:input { :type "textarea"  } ] "Endorsement " (:endorsement c) ]
    [:div 
       [:input { :type "submit" 
                    :data-theme "a"
                    :data-role "button"
                    :data-inline "true"
                    :value (:clear-label c)
                    :name (str "signature-btn-" (:name c))
                   } ]
     ] 
   
   [:h1 "Signature pad" ]
     [:div {:class "sig sigWrapper ccsig" }
       [:canvas {:class "pad" :width "198" :height "55" }  ]
       [:input {:type "hidden" :width "output" :class "output" }  ]
     ]
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

(defn- dbg
  [m]

  (if-let [b (config "verbose-collect-consents")]
    [:div.debug m ]))

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
  [c _]
  [:div.control.data-change
   (list (for [t (:meta-items c)] 
           (list
             (let [md (dsa/get-metadata t)]
                  [:div.ui-grid-b
                    [:div.ui-block-a (:label md) ]
                    [:div.ui-block-b (:value md) ]  
                          ;; note should use the value
                          ;; they entered previously 
                    [:div.ui-block-c 
                          [:input { :type "submit" 
                                    :data-theme "a"
                                    :data-role "button"
                                    :data-inline "true"
                                    :value "Change"
                                    :name (str "meta-data-btn-" (:mdid md))
                                   } ]
                    ]
                   ]))))
  ])

(defn policy-button
  [c _]
  [:div.control 
  [:input { :type "submit" 
            :data-theme "a"
            :data-role "button"
            :data-inline "true"
            :name (str "action-btn-" (:label c))
            :value (:label c)
           } ]
  ])

(defn text
  [c _]
  [:div.control.text
   (if (:title c) [:h1.title (:title c) ])
   (list (for [t (:text c)] [:p t ])) ])

(defn policy-checkbox
  "Display the checkbox.  Remember the state of the checkbox."
  [c m]
  [:div.control 
    (dbg (str "policy-checkbox-buttons m=" (pprint-str m)
              " lookup data [" (lookup-data c) "]"))
    (helper/checkbox-group {:name (:name c) :label (:label c) :value (lookup-data c) }) ])

(defn meta-items
  "TODO: Remember the state of the checkbox."
  [c m]
  [:div.control "ME Items" 
   [:div "the items are " (:meta-items) ] ])

(defn- show-meta
  [md]
  [:p "Meta data "  [:pre (pprint-str md) ]] )

(defn- process-control
  [c]

  (list 
    (let [ns "org.healthsciencessc.rpms2.consent-collector.collect-consents/"
              wname (str ns (:type c))
              fn (resolve (symbol wname)) 
              ;; lookup this widget in the data model and pass the value
              wmodel (helper/lookup-widget-by-name (:name c))
              ]
              (list
                 ;(debug "process-control " fn " ==> " c)
                 (if fn 
                     [:div 

                      (if-let [b (config "verbose-collect-consents")]
                        [:div.debug "Name: " (:name c) " WM " wmodel ])

                      [:div c ] (fn c wmodel) 
                     ]
                     [:div "Unrecognized control " 
                        [:span.control-type  (:type c) ] c ])
          
       (if-let [b (config "verbose-collect-consents")]
           [:div.debug  "name: " (:name c)  " WM " wmodel " " [:pre (pprint-str c)]])
                )

          )
        ))

(defn- process-section
  "Display section in a div"
  [s]
  [:div.section (map process-control (:contains s)) ])

(defn- display-page
  "Displays sections." 

  [p]

  (if (= nil p) 
    [:h1 "Unable to show page - missing page " 
         (if-let [pn (:page-name (session-get :collect-consent-status)) ]
             [:span.standout pn ]) ])
    [:div
       (dbg [:div.left "Page title " (:title p) ] )
       [:div (map process-section (:contains p)) ]
     ])

(defn- form-title
  [f]
  (get-in f [:header :title]))

(defn- get-named-page
  "Find page named 'n' in form 'f'"
  [f n]
  (first (filter #(= (:name %) n ) (:contains f) )))

(defn- get-consent-start-page
  "Returns the first page of the consent process."
  [f]
  (get-named-page f (:collect-start f)))

(defn- continue-form
  [ctx]
)

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

(defn- init-form-fields
  [form which-section n ]

  (debug "KKK init-form-fields A " (get-named-page form (which-section form)))
  (debug "KKK init-form-fields PAGE " (which-section form))
  (debug "KKK init-form-fields B " n " " which-section " FORM " form)
  ;; would be getting the nth form  instead of passing the form
  (let [p (get-named-page form (which-section form))]
    {:form form
     :state :begin 
     :page p
     :page-name (:name p)
     :current-form-number (+ n 1) 
    }))

(defn- get-next-form
  "Returns the nth form." 
  [n numforms]

  (if (and (< n numforms) 
           (< n 2)) ;; for now we only have two sample forms

      (do (debug "get-next-form: n = " n " Returning lewis black form" )
           dsa/lewis-blackman-form)
      nil))

(defn- advance-to-next-form
  "Advances to the next form.  Returns nil if no such form exists."
  []

  (let [s (session-get :collect-consent-status)]
    (if-let [next-form (get-next-form (+ 1 (:current-form-number s)) (:num-forms s))]
            (update-session 
              (init-form-fields (:form next-form) COLLECT_START_PAGE (:current-form-number s)))
            nil)))

(defn- initialize-capture-data-process
  []

  (helper/init-consents (dsa/sample-form) )
  (let [form (dsa/sample-form)
        m {:form (:form form)
           :state :begin 
           :page (get-consent-start-page (:form (dsa/sample-form)) )
           :current-form-number 0

           ;; this is a list of the actual forms
           ;;  :list-of-forms (session-get :protocols-to-be-filled-out)
           :num-forms (count (session-get :protocols-to-be-filled-out))
          }]
      (session-put! :collect-consent-status m )
      (session-put! :current-form (:form form) )
      (session-put! :current-form-data (:form form) )
      ;;(debug "192 initialize-capture-data-process " (pprint-str m))
    ))

(defn show-page
   "Collect and review consents proceesses. Displays current page"
  [ctx]

  ;; first time here, initialize 
  (if-let [s (session-get :collect-consent-status)]
    (debug "already initialized " (pprint-str (:name (:page s))))
    (initialize-capture-data-process)) 

  (let [s (session-get :collect-consent-status)]
    (helper/rpms2-page 
       (helper/collect-consent-form "/collect/consents"
           [:div 
            (if-let [b (config "verbose-collect-consents")]
              [:div.debug "DEBUG Page name " (:name (:page s))  
                 " current form " (:current-form-number s) " num forms " (:num-forms s) 
               [:div "Data " (pprint-str (session-get :model-data))]
               ])
            (display-page (:page s)) 
            ]
           [:div
           (if (:previous (:page s))
              (helper/standard-submit-button {:value "Previous" :name "previous" }))
           (helper/standard-submit-button {:value "Continue" :name "next" })
           ]) 
       :title (form-title (:form s) ))))

(defn view 
   "Collect and review consents processes. Displays current page"
  [ctx]
   
  (show-page ctx))

(defn- update-session
  "Merges the map, logs the new map, saves in session, and returns merged map."
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
  "Get parameters with name starting with string 's'"
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
      (helper/flash-and-redirect 
        (str
          "[Thank you for pressing that action button "  
             (pprint-str (get-matching-btns parms "action-btn-")) "]")
           "/collect/consents")

      (has-any? parms "signature-btn-")
      (helper/flash-and-redirect 
        (str
          "[Thank you for pressing that signature button "  
             (pprint-str (get-matching-btns parms "signature-btn-")) "]")
           "/collect/consents")

      (has-any? parms "meta-data-btn-")
      (helper/flash-and-redirect 
          (str "[Thank you for pressing that meta-data button " 
             (pprint-str (get-matching-btns parms "meta-data-btn-")) "]")
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
      (helper/myredirect "/collect/consents")

     (:review-confirmed s)
          (helper/myredirect "/view/unlock")

     :else
          (do (update-session {:page (get-named-page form (:summary-start form))
                               :page-name (:summary-start form)
                               :review-confirmed :true } )
             (view-finished ctx)))))


