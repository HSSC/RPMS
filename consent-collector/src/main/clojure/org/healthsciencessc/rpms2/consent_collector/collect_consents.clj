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


(defn- show-meta
  [md]
  [:p "Meta data "  [:pre (pprint-str md) ]] )


(defn- show-page
  [p]
  [:li "Page " (:title p) " has " (count (:contains p)) " sections" ])

(defn- show-nth-page
  [items page-num]
  (show-page (nth items page-num)))

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

(defn view 
   "Displays collect consents message"
  [ctx]
  (helper/rpms2-page 
    [:div
     [:h1 "Collect consents - # to be filled: "  
        (count (session-get :protocols-to-be-filled-out))  ]
     [:p "State: Page "  (session-get :current-page) " Section: " (session-get :current-section)]
     (show-current-form (session-get :current-form))
     ;;(show-meta (session-get :needed-meta-data))
     (helper/post-form "/collect/consents/" 
                       [:div "Finished with consents?" ]
                       (helper/standard-submit-button {:value "Continue" :name "done" }) ) ]
       :title "Collect consents unavailable."))

(defn perform 
  "Displays unimplemented message"
  [ctx]

  (if (= (session-get :form-status) :completed)
     ;; if all forms filled out, then go to the unlock screen
    (helper/rpms2-page "Go to unlock page - Unimplemented" :title "Collect consents unavailable.")
    (helper/rpms2-page "Fill out the next section - unimplemented" :title "Collect consents unavailable.")))
