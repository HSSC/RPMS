(ns org.healthsciencessc.rpms2.consent-collector.select-location
  (:require [hiccup.core :as hiccup]
            [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
            [org.healthsciencessc.rpms2.consent-collector.helpers :as helper]
            [hiccup.page-helpers :as hpages]
            [hiccup.form-helpers :as hform])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

(defn authorized-locations
  [user]
  (->> user
       :role-mappings
       (filter (comp #{"Consent Collector"} :name :role))
       (map :location)))

(defn select-location-radio-button-control
  "Returns HTML for selection location using radio buttons."
  [locs]

   [:fieldset {:data-role "controlgroup" }
 	"<legend>" (i18n :select-location-form-location-label) "</legend>"
   (for [l locs] 
	(let [rbname (str "radio-choice-" l)]
	 [:div	[:input {:name "location" :id rbname :type "radio" :value l } ]
		[:label {:for rbname} l ]  ]
	))
   ]
)

(defn perform
   "Save the location, then go to /view/select/lock-code"
   [{{:keys [location]} :body-params } ]

   (debug "Location has been selected: " location)
   (session-put! :location location)
   (helper/myredirect "/view/select/lock-code"))

(defn view 
  "Based on the number of authorized locations for logged in user: 
     1. If no locations, redirects to view-not-authorized   
     2. If 1 location, that location and it's parent organization is set 
  	in session context and user is redirected to view-select-lock-code.
     3. If more than one location, displays the page to select the locations.

     Pivotal Tracker: https://www.pivotaltracker.com/story/show/26014553"
  [_]
  (try 
    (let [locs-data (authorized-locations (session-get :user))
          locs-names (map :name locs-data)]
      (debug "default-get-view-select-location -> locs = " locs-names)
      (if (empty? locs-names)
        (helper/myredirect "/view/not-authorized")
        (if (= (count locs-names) 1)  
          (let [l (first locs-data)]
            (debug "Using Location/Org information: | " l "|")
            (session-put! :org-location l)
            (helper/myredirect "/view/select/lock-code"))
          (helper/rpms2-page 
   		(helper/standard-form "POST" (helper/mypath "/view/select/location" )
		      (select-location-radio-button-control locs-names)  
		      (helper/submit-button "select-location-form") )

                      :pageheader (i18n :hdr-select-location)))))
    (catch Exception ex (str "select-location view Failed: " ex))))

