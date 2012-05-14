(ns org.healthsciencessc.rpms2.consent-collector.select-location
  (:require [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa])
  (:require [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
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
       
(defn perform
   "Save location, then go to /view/select/lock-code"
   [{{:keys [location]} :body-params } ]

   (if (or (empty? location) 
           (= nil location))
        (do (flash-put! :header (i18n "select-location-form-location-required"))
            (helper/myredirect "/view/select/location")) 
        (do 
           (session-put! :location location)
           (helper/myredirect "/view/select/lock-code"))))


(defn- select-location-form
  "Display the select location form."
  [locs-names]
  (helper/rpms2-page 
    (helper/post-form "/view/select/location"
      (list
         [:fieldset {:data-role "controlgroup" }
 	  [:div.left (i18n :select-location-form-location-label) ]
          (for [l locs-names] 
	    (let [rbname (str "radio-choice-" l)]
	      [:div	
                [:input {:name "location" :id rbname :type "radio" :value l } ]
	        [:label {:for rbname :class "labelclass" } l ]  ]
	)) ])

        (helper/standard-submit-button { 
                    :value (i18n "select-location-form-submit-button") 
                    :name "select-location-submit-button" } ))

    :title (i18n :hdr-select-location)))

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
      (if (or (= nil locs-names) 
              (= nil (first locs-names))
              (empty? locs-names))
          (helper/myredirect "/view/not-authorized")
          (if (= (count locs-names) 1)  
          (let [l (first locs-data)]
            (session-put! :org-location l)
            (session-put! :location (first locs-names))
            (session-put! :org-name (get-in (first locs-data) [:organization :name])) 
            (helper/myredirect "/view/select/lock-code"))
            (select-location-form locs-names)))))
