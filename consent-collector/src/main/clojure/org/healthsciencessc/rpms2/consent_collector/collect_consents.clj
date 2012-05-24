(ns org.healthsciencessc.rpms2.consent-collector.collect-consents
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! destroy-session! ]])
  (:use [clojure.tools.logging :only (debug info warn error)])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.pprint :only (pprint)])
  (:use [org.healthsciencessc.rpms2.consent-collector.debug :only [debug! pprint-str]])
  (:use [org.healthsciencessc.rpms2.consent-collector.config :only [config]])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

(def ^:const ^:private COLLECT_START_PAGE :collect-start)
(def ^:const ^:private REVIEW_START_PAGE :summary-start)

(defn signature
  [c]
  [:div.control.signature "Your signature is requested: " 
    [:div "Guarantor " (:name c)]
    [:div [:input { :type "textarea"  } ] "Endorsement " (:endorsement c) ]
    [:div 
       [:input { :type "submit" 
                    :data-theme "a"
                    :data-role "button"
                    :data-inline "true"
                    :value (:clear-label c)
                    :name (:name c) } ]
     ] ])

(defn policy-text
  "A PolicyText widget generates title and paragraph from a specific Policy"
  [c]
  [:div.control.policy-text
   (list 
     (let [policy (dsa/get-policy (:policy c))]

     ;; Display title if :render-title is missing or true 
     ;; and policy has a title
     (list 
       (if (and (not (= (:render-title c) false)) 
                (:title policy))
           [:div [:h1.title (:title policy)]])

     ;; Display text if :render-text is missing or true and policy has text
       (if (and (not (= (:render-text c) false))
                (:text policy))
             (map (fn [tt] [:div.text tt ]) (:text policy)))

     (if (not (= (:render-media c) false)) 
       [:div.render-media "Render media controls here" ]) ))) ])

(defn policy-choice-buttons
  "Creates two buttons that allow you to opt in or opt out of one or more
  policies."
  [c]
  [:div.control.policy-choice-buttons 
    [:fieldset {:data-role "controlgroup" }
      (helper/radio-btn (:name c) (:true-label c)) 
      (helper/radio-btn (:name c) (:false-label c))
     ]
   ])

(defn data-change
  [c]
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
                                    :name (:name c) } ]
                    ]
                   ]))))
  ])

(defn policy-button
  [c]
  [:div.control 
  [:input { :type "submit" 
            :data-theme "a"
            :data-role "button"
            :data-inline "true"
            :name (:label c)
            :value (:label c)
           }  ]
  ])

(defn text
  [c]
  [:div.control.text
   (if (:title c) [:h1.title (:title c) ])
   (list (for [t (:text c)] [:p t ])) ])

(defn policy-checkbox
  [c]
  [:div.control 
   [:div {:data-role "fieldcontain" }
   [:input {:type "checkbox" :id (:name c) } ]
   [:label {:for (:name c) } (:label c) ]] ])

(defn meta-items
  [c]
  [:div.control "ME Items" 
   [:div "the items are " (:meta-items) ] ])

(defn- show-meta
  [md]
  [:p "Meta data "  [:pre (pprint-str md) ]] )

(defn- process-control
  [c]

  (list (let [ns "org.healthsciencessc.rpms2.consent-collector.collect-consents/"
              wname (str ns (:type c))
              fn (resolve (symbol wname )) ]
              (list
                 ;(debug "process-control " fn " ==> " c)
                 (if fn 
                     [:div [:div c ] (fn c) ]
                     [:div "Unrecognized control " 
                        [:span.control-type  (:type c) ] c ])))
       (if-let [b (config "verbose-collect-consents")]
           [:pre (pprint-str c)])))

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
      (if-let [b (config "verbose-collect-consents")]
        [:h1.left (:title p) ] )
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
  (flash-put! :header "Finished collecting consents.")
  (let [s (session-get :collect-consent-status)]
    (helper/rpms2-page 
       (helper/post-form "/collect/consents"
           [:div 
              [:div.finished1 "Thank You! " ]
              [:div.finished2 (str "Your selected " (helper/org-protocol-label) "s are complete.") ]
              [:div.finished3 "Return the device to the clerk." ] ]
           (helper/standard-submit-button {:value "Continue" :name "next" })) 
       :title "Consents Complete" )))

(defn- capture-data
  [ctx]

  (debug "capture data " ctx)
)

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
  []

  (let [s (session-get :collect-consent-status)]
    (if-let [next-form (get-next-form (+ 1 (:current-form-number s)) (:num-forms s))]
        (let [updated (merge s (init-form-fields (:form next-form) COLLECT_START_PAGE (:current-form-number s)))]
          (do
            (debug "198 next-form " (pprint-str updated))
            (session-put! :collect-consent-status updated )
            updated))
          nil)
    ))

(defn- initialize-capture-data-process
  []

  (let [form (dsa/sample-form)
        m {:form (:form form)
           :state :begin 
           :page (get-consent-start-page (:form (dsa/sample-form)) )
           :model {}
           :current-form-number 0

           ;; this is a list of the actual forms
           ;;  :list-of-forms (session-get :protocols-to-be-filled-out)
           :num-forms (count (session-get :protocols-to-be-filled-out))
          }]
   (session-put! :collect-consent-status m )
   (debug "192 initialize-capture-data-process " (pprint-str m))))

(defn show-page
   "Collect and review consents proceesses. Displays current page"
  [ctx]

  ;; first time here, initialize 
  (if-let [s (session-get :collect-consent-status)]
    (debug "202 view PAGE " (pprint-str (:page s)) " STATE " (pprint-str (:state s)) " ALL " (pprint-str s))
    (initialize-capture-data-process)) 

  (let [s (session-get :collect-consent-status)]
    (helper/rpms2-page 
       (helper/collect-consent-form "/collect/consents"
           [:div 
            (if-let [b (config "verbose-collect-consents")]
              [:div.debug "DEBUG Page name " (:name (:page s))  
                 " current form " (:current-form-number s) " num forms " (:num-forms s) ])
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
  [m]
  (let [s (session-get :collect-consent-status)]
       (session-put! :collect-consent-status (merge s m))))

(defn perform
  "Collect consents. "

  [{parms :body-params :as ctx}]

  (let [s (session-get :collect-consent-status)
        form (:form s) 
        nxt (if-let [nxt (:next (:page s))] (get-named-page form nxt) nil) 
        prev (if-let [nxt (:previous (:page s))] (get-named-page form nxt) nil) ]

    (debug "258 perform nxt " nxt)
    (capture-data ctx)

    ;; see if previous or continue was pressed
    ;; either go to the next page or show end of collection page
    (cond 

      ;; if previous button pressed and prev page available
      (and (:previous parms)
           prev)
      (do (debug "Going to previous page: " (:name prev))
          (update-session {:page prev :page-name (:name prev) })
          (show-page {} )) ;; (helper/myredirect "/collect/consents"))

      ;; if next page available
      nxt 
        (do 
          (debug "Going to next page: " (:name nxt))
          (update-session {:page nxt :page-name (:name nxt) })
          (helper/myredirect "/collect/consents"))

      ;; At the end of the current form, so set current page to start of next form,
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
