(ns org.healthsciencessc.rpms2.consent-collector.select-location
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

(defn authorized-locations
  [user]
  (debug "authorized-locations " (helper/username))
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

   ;; ensure location has been selected. also could do this in the javascript
   (if (or (empty? location) 
           (= nil location))
        (do (debug "no location selected") 
            (flash-put! :header (i18n "select-location-form" "location-required"))
            (helper/myredirect "/view/select/location")) 
        (do (debug "Location has been selected: " location)
           (session-put! :location location)
           (helper/myredirect "/view/select/lock-code"))))

(defn view 
  "Based on the number of authorized locations for logged in user: 
     1. If no locations, redirects to view-not-authorized   
     2. If 1 location, that location and it's parent organization is set 
  	in session context and user is redirected to view-select-lock-code.
     3. If more than one location, displays the page to select the locations.

     Pivotal Tracker: https://www.pivotaltracker.com/story/show/26014553"
  [_]
  (let [locs-data (authorized-locations (session-get :user))
          locs-names (map :name locs-data)]
      ;;(debug "location/view  -> data = " locs-data)
      (if (or (= nil locs-names) 
              (= nil (first locs-names))
              (empty? locs-names))
          (helper/myredirect "/view/not-authorized")
        (if (= (count locs-names) 1)  
          (let [l (first locs-data)]
            (session-put! :org-location l)
            (session-put! :location (first locs-names))
            (session-put! :org-name (get-in (first locs-data) [:organization :name])) 
            ;(flash-put! :header (str "one location " l ) )
            (helper/myredirect "/view/select/lock-code"))
          (helper/rpms2-page 
   		(helper/standard-form "POST" (helper/mypath "/view/select/location" )
#_(flash-put! :header (str "Multiple locations " (pr-str locs-data )) )

		      (select-location-radio-button-control locs-names)  
		      (helper/submit-button "select-location-form") )
                      :title (i18n :hdr-select-location))))))

