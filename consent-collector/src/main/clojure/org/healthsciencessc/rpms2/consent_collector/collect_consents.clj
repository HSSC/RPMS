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
           [:div [:h1.text (:title policy)]])

     ;; Display text if :render-text is missing or true and policy has text
       (if (and (not (= (:render-text c) false))
                (:text policy))
           (list (for [tt (:text policy)] [:div.text tt ])))

     (if (not (= (:render-media c) false)) 
       [:div "Render media controls here" ]) ))) ])

(defn policy-choice-buttons
  "Creates two buttons that allow you to opt in or opt out of one or more
  policies."
  [c]
  [:div.control.policy-choice-buttons 
    [:fieldset {:data-role "controlgroup" }
      (helper/radio-btn (:name c) (:false-label c))
      (helper/radio-btn (:name c) (:true-label c)) ]
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
  [:div.control 
   (if (:title c) [:h1 (:title  c) ])
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

(defn- display-page
  [p]

  (if (= nil p) 
    [:h1 "Unable to show page - missing page " 
     (if-let [pn  (:page-name (session-get :collect-consent-status)) ]
         [:span.standout pn ])]
    [:div.left
     [:h1.left (:title p) ] 
     (list (for [section (:contains p)]
       (list (for [c (:contains section) ]
               (list (process-control c)))))) ]))

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
              [:div.finished2 "Your selected protocols are complete." ]
              [:div.finished3 "Return the device to the clerk." ] ]
           (helper/standard-submit-button {:value "Continue" :name "next" })) 
       :title "Consents Complete" )))

(defn- capture-data
  [ctx]

  (debug "capture data " ctx)
)

(defn view 
   "Displays collect consents message."
  [ctx]

  ;; first time here, initialize :collect-consent-status map
  (if-let [s (session-get :collect-consent-status)]
    (do 
        (debug "266 PAGE " (pprint-str (:page s)) 
               " STATE " (pprint-str (:state s)))
        (debug "266 ALREADY INITIALIZED ALL " (pprint-str s)))
    (do (session-put! :collect-consent-status 
             {:form (:form (dsa/sample-form))
              :state :begin 
              :page (get-consent-start-page (:form (dsa/sample-form)) )
              :model {}
              })
        (debug "274 INITIALIZED " (pprint-str (session-get :collect-consent-status)))
      )) 

  (let [s (session-get :collect-consent-status)]
    (helper/rpms2-page 
       (helper/post-form "/collect/consents"
           [:div (display-page (:page s)) ]
           [:div
           (if (:previous (:page s))
              (helper/standard-submit-button {:value "Previous" :name "previous" }))
           (helper/standard-submit-button {:value "Continue" :name "next" })
           ]) 
       :title (form-title (:form s) ))))


(defn- update-session
  [m]
  (let [s (session-get :collect-consent-status)]
       (session-put! :collect-consent-status (merge s m))))

(defn perform
  "Collect consents.
  TODO: See if next or previous page was submitted previous page."

  [{parms :body-params :as ctx}]

  (debug "perform BTN CTX " ctx)
  (debug "perform BTN PARMS " parms)
  (debug "perform BTN PREVIOUS? " (:previous parms))
  (debug "perform BTN DONE? " (:done parms))
  (let [s (session-get :collect-consent-status)
        form (:form s) 
        nxt (if-let [nxt (:next (:page s))] (get-named-page form nxt) nil) 
        prev (if-let [nxt (:previous (:page s))] (get-named-page form nxt) nil)]

    (debug "258 perform-in-form nxt " nxt)
    (capture-data ctx)

    ;; see if previous or continue was pressed
    ;; either go to the next page or show end of collection page
    (if (and (:previous parms)
             prev)
      (do (debug "Going to previous page: " (:name prev))
          (update-session {:page prev :page-name (:name prev) })
          (helper/myredirect "/collect/consents"))

      (if nxt 
        (do 
          (debug "Going to next page: " (:name nxt))
          (update-session {:page nxt :page-name (:name nxt) })
          (helper/myredirect "/collect/consents"))

      ;; the next page, set current page to start of review 
      (if (:review-confirmed s)
         (helper/myredirect "/view/unlock")
         (do (update-session {:page (get-named-page form (:summary-start form))
                              :page-name (:summary-start form)
                              :review-confirmed :true } )
             (view-finished ctx)))))))
