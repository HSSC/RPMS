(ns org.healthsciencessc.rpms2.consent-collector.collect-consents
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! destroy-session! ]])
  (:use [clojure.tools.logging :only (debug info warn error)])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.pprint :only (pprint)])
  (:use [org.healthsciencessc.rpms2.consent-collector.debug :only [debug! pprint-str]])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))


(defn signature
  [b]
  [:div.control "Your signature is requested: " 
   [:pre (pprint-str b)]
  ])

(defn policy-text
  [c]
  [:div.control "Policy Text" 
   (if (:render-title c) [:h1 "Render Title" ])
   [:pre (pprint-str c)]
  ])

(defn policy-choice-buttons
  [c]
  [:div.control "Policy choice buttons" 
    [:div "Polic text " "Lookup Text for " (:policy c)]
    [:div "False: " (:false-label c)]
    [:div "True: " (:true-label c)]
    [:pre (pprint-str c)]
  ])

(defn data-change
  [c]
  [:div.control "Data change" 
   [:pre (pprint-str c)]
  ])

(defn text
  [c]
  [:div.control "Text " 
   [:div "Text: " (:text c) ]
   [:div "Title " (:title  c) ]
   [:div "Name " (:name  c) ]
   [:pre (pprint-str c)]
  ])


(defn policy-checkbox
  [c]
  [:div.control "Policy Checkbox " 
   [:pre (pprint-str c)]
  ])


(defn meta-items
  [c]
  [:div.control "ME Items" 
   [:div "the items are " (:meta-items) ]
   [:pre (pprint-str c)]
  ])


(defn- show-meta
  [md]
  [:p "Meta data "  [:pre (pprint-str md) ]] )

(defn- show-page
  [p]
  [:li "Page " (:title p) " has " (count (:contains p)) " sections" ])

(defn- process-control
  [c]

  (list (let [s (:type c) 
        ns "org.healthsciencessc.rpms2.consent-collector.collect-consents/"
        wname (str ns (:type c))
        _ (println "wname is " wname)
        fn (resolve (symbol wname )) 
        _ (println "fn is " fn)
        ]
      [:span.control-type (:type c) ]
      (if fn [:div [:div c ] (fn c) ]
        [:div "Unrecognized control " 
          [:span.control-type  (:type c) ] c ])
    )))

(defn- show-page-sections
  [p]
  ;;(debug "SHOW PAGE SECTIONS " p)
  [:div.left
     [:h1.left (:title p) " #" (count (:contains p)) " sections" ]
     [:ol
     (list (for [section (:contains p)]
       (list [:li [:span.standout "section: " section  ]
             ;; for each 
             (list (for [c (:contains section) ]
               (list
                 ;;(println "PRINT THE CONTROL " c)
                 ;;[:h2 "Control " c ]
                 (process-control c))
             ))])))]
  ])

(defn- show-nth-page
  [items page-num]
  (show-page (nth items page-num)))

(defn- form-title
  [f]
  (get-in f [:form :header :title]))

(defn- get-named-page
  [f pname]
  (first (filter #(= (:name %) pname ) (:contains f) )))

(defn- get-first-page
  "Finds page named by the :collect-start property."

  [f]
  (let [pname (get-in f [:collect-start])
        cur-page (first (filter #(= (:name %) pname ) (:contains f) )) ] 
    ;;(debug "f " f)
    ;;(debug "get-first-page contains " (:contains f))
    ;;(debug "get-first-page curr page is " cur-page)
    cur-page))

(defn- show-current-form 
  [f]
  ;;[:p "Current form "  [:pre (pprint-str f) ]] 
  [:div
   (list (let [title (get-in f [:form :header :title])
               footer (get-in f [:form :footer :title])
               items (get-in f [:form :contains])]
           (list
             [:div "title " title ]
             ;;[:div "footer " footer ]
           (for [n (range (count items))]
               (show-nth-page items n))))
   )])


(defn- show-current-form-sections
  [f]
  ;;[:p "Current form "  [:pre (pprint-str f) ]] 
  [:div
   (list (let [title (get-in f [:form :header :title])
               footer (get-in f [:form :footer :title])
               items (get-in f [:form :contains])]
           (list
             [:div "title " title ]
             ;;[:div "footer " footer ]
           (for [n (range (count items))]
               (show-page-sections (nth items n)))))
   )])

(defn- show-form-page
  [f cur-page]
  [:div
   (list (let [title (get-in f [:form :header :title])
               footer (get-in f [:form :footer :title])
               items (get-in f [:form :contains])
              _ (debug "CUR-PAGE " cur-page)
              _ (println "CUR-PAGE " cur-page)
              ]
           (list
             [:div "Form title " title " on page " (:name cur-page)]
             (show-page-sections cur-page)
             ))
   )])


(defn- section-title
  []
  (let [s (session-get :collect-consent-status)]
    [:div 
     (debug "section title " (pprint-str s) )
     [:p "State: Page "  (:current-page s) ]
     ]))

(defn- continue-form
  [ctx]
)


(defn- view-start-collection
  [ctx]

  (helper/rpms2-page 
    [:div
     [:h1 "Collect consents - # to be filled: "  
        (count (session-get :protocols-to-be-filled-out))  ]
     (section-title)
     (show-current-form (:form (session-get :collect-consent-status)) )
     ;;(show-meta (session-get :needed-meta-data))
     (helper/post-form "/collect/consents/" 
                       [:div "Begin Collect Consent process" ]
                       (helper/standard-submit-button {:value "Continue" 
                                                       :name "done" }) ) ]
       :title "Start Collect consents"))

(defn- view-in-form
  [ctx]
  (let [s (session-get :collect-consent-status)]
    (helper/rpms2-page 
       (helper/post-form "/collect/consents"
           [:div (show-form-page (:form s) (:page s)) ]
           (helper/standard-submit-button 
              {:value "Continue" :name "done" }) ) 
       :title (form-title (:form s) ))))
 
(defn- view-other 
  [ctx]

  (helper/rpms2-page "PERFORM OTHER " :title "Perform other"))

(defn view 
   "Displays collect consents message"
  [ctx]

  (let [s (session-get :collect-consent-status)
        state (:state s)]

    (cond (= state :begin)
          (view-start-collection ctx)

          (= state :in-form)
          (view-in-form ctx)
    :else
          (view-other ctx)
   )))
  

(defn- perform-in-form
  "We are in the form.  There should be a page object."
  [ctx]
  (let [s (session-get :collect-consent-status)
        form (get-in s [:form :form]) 
        next-page (if-let [nxt (:next (:page s))] (get-named-page form nxt) nil)
        ]

    (if next-page 
      (do (session-put! :collect-consent-status 
                  (merge s {:state :in-form 
                            :page next-page }) )
          (helper/myredirect "/collect/consents"))
      (helper/rpms2-page 
         [:div "NO MORE PAGES " ]
      :title (form-title (:form s) )))))
 
(defn- perform-start-collection
  "Prepare to start collecting consents.
  Set the collection status (:collect-consent-status)
  to the first page."
  [ctx]

  (let [s (session-get :collect-consent-status)
        form (get-in s [:form :form]) ]
    (session-put! :collect-consent-status 
                  (merge s {:state :in-form 
                            :page (get-first-page form)}) )
    (helper/myredirect "/collect/consents")))

(defn- perform-finished-collection
  [ctx]

  (helper/rpms2-page "Fill out the next section - unimplemented" :title "Collect consents unavailable."))

(defn- perform-other 
  [ctx]

  (helper/rpms2-page "PERFORM OTHER " :title "Perform other"))


(defn perform 
  "Displays collect consent information based on the state"
  [ctx]

  (debug "collect-consents/perform ctx " ctx)
  (let [s (session-get :collect-consent-status)
        state (:state s)]

    (cond (= state :begin)
          (perform-start-collection ctx)

          (= state :in-form)
          (perform-in-form ctx)
    :else
          (perform-other ctx)
   )))


