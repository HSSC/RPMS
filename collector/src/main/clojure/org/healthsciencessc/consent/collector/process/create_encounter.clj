(ns org.healthsciencessc.consent.collector.process.create-encounter
  (:refer-clojure :exclude [root])
  (:require [org.healthsciencessc.consent.client.core :as services]
            [org.healthsciencessc.consent.client.whoami :as whoami]
            [org.healthsciencessc.consent.collector.respond :as respond]
            [org.healthsciencessc.consent.collector.state :as state]
            [org.healthsciencessc.consent.collector.text :as text]
            [org.healthsciencessc.consent.collector.ui.action :as action]
            [org.healthsciencessc.consent.collector.ui.form :as form]
            [org.healthsciencessc.consent.collector.ui.layout :as layout]
            [org.healthsciencessc.consent.domain.roles :as roles]
            [org.healthsciencessc.consent.domain.tenancy :as tenancy]
            [pliant.webpoint.request :as endpoint]
            [clojure.string :as string])
  (:use     [pliant.process :only [defprocess as-method]]))


(def fields [{:name :encounter-id :type :text :label (text/text :encounter.id.label) :contain true}
             {:name :date :type :date :label (text/text :encounter.date.label) :contain true}])

(def form-options {:method :put
                   :url "/api/encounter"})

;; Register The Create Encounter View
(defprocess view-create-encounter
  "Creates a view for addings an encounter."
  [ctx]
  (if (whoami/identified?)
    (layout/render-page ctx {:title (text/encounter-text :create.encounter.title) :pageid "CreateEncounter"} 
                   (form/dataform form-options 
                                  (form/render-fields {:fields {:encounter-id {:label (text/encounter-text :encounter.id.label)}
                                                                :date {:label (text/encounter-text :encounter.date.label)}}} 
                                                      fields {})
                                  (action/wrapper
                                    (action/form-submit {:label (text/text :action.create.label)}))))
    (respond/forbidden-view ctx)))

(as-method view-create-encounter endpoint/endpoints "get-view-create-encounter")


;; Register API Service To Persist The Encounter
(defprocess api-create-encounter
  "Performs the encounter search "
  [ctx]
  (if (whoami/identified?)
    (let [{:keys [encounter-id date] :as data} (:body-params ctx)]
      (cond
        (string/blank? encounter-id) (respond/with-error (text/encounter-text :create.encounter.id.required))
        (string/blank? date) (respond/with-error (text/encounter-text :create.encounter.date.required))
        :else
        (let [location-id (:id (state/get-location))
              consenter-id (:id (state/get-consenter))
              encounter (services/add-encounter location-id consenter-id data)]
          (state/set-encounter encounter)
          (respond/with-actions {:encounter encounter :view-url "/view/select/protocol" :reset false} "setEncounter" "changeView"))))
    (respond/forbidden-view ctx)))

(as-method api-create-encounter endpoint/endpoints "put-api-encounter")
