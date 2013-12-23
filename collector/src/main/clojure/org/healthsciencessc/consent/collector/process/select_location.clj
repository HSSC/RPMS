(ns org.healthsciencessc.consent.collector.process.select-location
  (:refer-clojure :exclude [root])
  (:require [org.healthsciencessc.consent.client.core :as services]
            [org.healthsciencessc.consent.client.whoami :as whoami]
            [org.healthsciencessc.consent.collector.respond :as respond]
            [org.healthsciencessc.consent.collector.state :as state]
            [org.healthsciencessc.consent.collector.text :as text]
            [org.healthsciencessc.consent.collector.ui.action :as action]
            [org.healthsciencessc.consent.collector.ui.form :as form]
            [org.healthsciencessc.consent.collector.ui.layout :as layout]
            [org.healthsciencessc.consent.common.roles :as roles]
            [org.healthsciencessc.consent.common.tenancy :as tenancy]
            [pliant.webpoint.request :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))


(def fields [{:name :location :type :radio :label "Location" :autofocus false}])

(def form-options {:method :post
                   :url "/api/select/location"})

;; Register The Login View
(defprocess view-select-location
  "Creates a view of locations to be selected."
  [ctx]
  (if (whoami/identified?)
    (let [user (whoami/get-user)
          location (state/get-location)
          mappings (roles/consent-collector-mappings user)
          items (sort #(compare (vec (map val %1)) (vec (map val %2)))
                      (distinct (for [mapping mappings] 
                            {:order (#(cond (nil? %) 1000 
                                            (and (string? %) (empty? %)) 1000 
                                            (string? %) (.parseInt java.lang.Integer 1000) 
                                            :else %) (get-in mapping [:location :order]))
                             :label (get-in mapping [:location :name])
                             :value (get-in mapping [:location :id]) })))
          data {:location (:id location)}]
      (layout/render-page ctx {:title (text/location-text :select.location.title) :pageid "SelectLocation"} 
                   (form/dataform form-options 
                                  (form/render-fields {:fields {:location {:label (text/location-text :select.location.locations.label)
                                                                           :items items}}} 
                                                      fields data)
                                  (action/wrapper
                                    (action/form-submit {:label (text/text :action.select.label)})))))
    (respond/forbidden-view ctx)))

(as-method view-select-location endpoint/endpoints "get-view-select-location")

;; Register The Location Selection Service Process
(defprocess api-select-location
  "Performs the login "
  [ctx]
  (if (whoami/identified?)
    (if-let [location-id (get-in ctx [:body-params :location])]
      (let [current-location (state/get-location)
            location (services/get-location location-id)
            url (if current-location "/view/search/consenter" "/view/select/lockcode")]
        (if location
          (do 
            (state/set-location location)
            (respond/with-actions {:location location :view-url url :reset false} "setLocation" "changeView"))
          (respond/with-error ctx (text/location-text :select.location.message.notvalid))))
          (respond/with-error ctx (text/location-text :select.location.message.notvalid)))
    (respond/forbidden-api ctx)))

(as-method api-select-location endpoint/endpoints "post-api-select-location")
