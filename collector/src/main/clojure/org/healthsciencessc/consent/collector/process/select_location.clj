(ns org.healthsciencessc.consent.collector.process.select-location
  (:refer-clojure :exclude [root])
  (:require [org.healthsciencessc.consent.collector.respond :as respond]
            [org.healthsciencessc.consent.collector.state :as state]
            [org.healthsciencessc.consent.collector.text :as text]
            [org.healthsciencessc.consent.collector.process.authorize :as auth]
            [org.healthsciencessc.consent.collector.ui.action :as action]
            [org.healthsciencessc.consent.collector.ui.form :as form]
            [org.healthsciencessc.consent.collector.ui.layout :as layout]
            
            [org.healthsciencessc.consent.client.core :as services]
            
            [org.healthsciencessc.consent.domain.roles :as roles]
            [org.healthsciencessc.consent.domain.tenancy :as tenancy]
            
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))


(def fields [{:name :location :type :radio :label "Location" :autofocus true}])

(def form-options {:method :post
                   :url "/api/select/location"})

;; Register The Login View
(defprocess view-select-location
  "Creates a view of locations to be selected."
  [ctx]
  (if (auth/is-authenticated?)
    (let [user (state/get-user)
          location (state/get-location)
          mappings (roles/consent-collector-mappings user)
          items (distinct (for [mapping mappings] 
                            {:value (get-in mapping [:location :id]) :label (get-in mapping [:location :name])}))
          data {:location (:id location)}]
      (layout/render-page ctx {:title (text/location-text :select.location.title) :pageid "SelectLocation"} 
                   (form/dataform form-options 
                                  (form/render-fields {:fields {:location {:label (text/location-text :select.location.locations.label)
                                                                           :items items}}} 
                                                      fields data)
                                  (action/form-submit {:label (text/text :action.select.label)}))))
    (respond/forbidden-view ctx)))

(as-method view-select-location endpoint/endpoints "get-view-select-location")

;; Register The Location Selection Service Process
(defprocess api-select-location
  "Performs the login "
  [ctx]
  (if (auth/is-authenticated?)
    (if-let [location-id (get-in ctx [:body-params :location])]
      (let [current-location (state/get-location)
            location (services/get-location location-id)
            url (if current-location "/view/search/consenter" "/view/select/lockcode")]
        (if location
          (do 
            (state/set-location location)
            (respond/with-actions {:location location :view-url url :reset false} "setLocation" "changeView"))
          (respond/with-error (text/location-text :select.location.message.notvalid))))
          (respond/with-error (text/location-text :select.location.message.notvalid)))
    (respond/forbidden-view ctx)))

(as-method api-select-location endpoint/endpoints "post-api-select-location")
