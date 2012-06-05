(ns org.healthsciencessc.rpms2.consent-collector.select-location
  (:require [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa])
  (:require [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [org.healthsciencessc.rpms2.consent-collector.debug :only [debug! pprint-str]])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

(defn perform
   "Save location, then go to /view/select/lock-code"
   [{{:keys [location]} :body-params :as ctx } ]

   (debug "select-location/perform: " location " ctx " ctx)

   (if (empty? location)
       (helper/flash-and-redirect 
          (str (helper/org-location-label) " is required")
               "/view/select/location") 
       (let [rmapping (:role-mappings (session-get :user))
             t1 (filter #(= location (get-in % [:location :name])) rmapping)
             selected-loc (:location (first t1)) ]
          (do
            (session-put! :org-location selected-loc)
            (session-put! :location location)
            (helper/myredirect "/view/select/lock-code")))))


(defn- select-location-form
  "Display select location form."
  [locs-names locs-data]
  (helper/clear-patient)
  (helper/clear-location)
  (helper/rpms2-page 
    (helper/post-form "/view/select/location"
      [:fieldset {:data-role "controlgroup" }
        [:div.left (str "Available " (helper/org-location-label) "(s)") ]
        (map (fn [l] (helper/radio-btn "location" l )) (distinct locs-names))
      ]

      (helper/standard-submit-button { 
                :value (str "Select " (helper/org-location-label))
                :name "select-location" } ))
    :title (str "Select " (helper/org-location-label))))

(defn view 
  "Based on the number of authorized locations for logged in user: 
     1. If no locations, redirects to view-not-authorized   
     2. If 1 location, that location and it's parent organization is set 
  	in session context and user is redirected to view-select-lock-code.
     3. If more than one location, displays the page to select the locations.

     Pivotal Tracker: https://www.pivotaltracker.com/story/show/26014553"
  [_]
  (let [locs-data (helper/authorized-locations)
          locs-names (map :name locs-data)]
      (if (or (= nil locs-names) 
              (= nil (first locs-names))
              (empty? locs-names))
          (helper/myredirect "/view/not-authorized")
          (if (= (count (distinct locs-names)) 1)  
              (let [l (first locs-data)]
                (session-put! :org-location l)
                (session-put! :location (first locs-names))
                (session-put! :org-name (get-in (first locs-data) [:organization :name])) 
               (helper/myredirect "/view/select/lock-code"))
             (select-location-form locs-names locs-data)))))
