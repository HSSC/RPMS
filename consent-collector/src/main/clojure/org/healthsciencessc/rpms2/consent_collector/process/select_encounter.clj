(ns org.healthsciencessc.rpms2.consent-collector.process.select-encounter
  (:refer-clojure :exclude [root])
  (:require [org.healthsciencessc.rpms2.consent-collector.common :as common]
            [org.healthsciencessc.rpms2.consent-collector.process.authorize :as auth]
            [org.healthsciencessc.rpms2.consent-collector.respond :as respond]
            [org.healthsciencessc.rpms2.consent-collector.state :as state]
            [org.healthsciencessc.rpms2.consent-collector.text :as text]
            [org.healthsciencessc.rpms2.consent-collector.ui.action :as action]
            [org.healthsciencessc.rpms2.consent-collector.ui.content :as uicontent]
            [org.healthsciencessc.rpms2.consent-collector.ui.form :as form]
            [org.healthsciencessc.rpms2.consent-collector.ui.grid :as grid]
            [org.healthsciencessc.rpms2.consent-collector.ui.layout :as layout]
            
            [org.healthsciencessc.rpms2.consent-client.core :as services]
            
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))

(def fields [])


(def fields [{:name :id :type :hidden}
             {:name :encounter-id :type :text :label (text/text :encounter.id.label)}
             {:name :date :type :date :label (text/text :encounter.date.label)}])


;; Register The Select Consenter View
(defprocess view-select-encounter
  "Creates a view of the search results to select a encounter from."
  [ctx]
  (if (auth/is-authenticated?)
    (let [consenter-id (:id (state/get-consenter))
          consenter (services/get-consenter consenter-id)
          encounters (:encounters consenter)]
      (cond
        (nil? consenter) 
          (respond/redirect ctx "/view/search/consenter")
        (nil? (seq encounters)) 
          (respond/redirect ctx "/view/create/encounter")
        :else
          (let [datelabel (text/encounter-text :encounter.date.label)
                items (for [e encounters] {:data e :label (:encounter-id e) :sublabel (str datelabel ": " (:date e))})]
                  (layout/render-page ctx {:title (text/encounter-text :select.encounter.title) :pageid "SelectEncounter"}
                     (uicontent/datalistview {:autoselect :first}
                                             (form/dataform {:method :post :url "/api/select/encounter"}
                                                            (form/render-fields 
                                                              {:fields {:encounter-id {:label (text/encounter-text :encounter.id.label)}
                                                                        :date {:label (text/encounter-text :encounter.date.label)}}}
                                                              fields {})
                                                            (grid/gridify {}
                                                              (action/next-view {:url "/view/create/encounter" :method :get
                                                                                 :label (text/consenter-text :select.encounter.new.label)})
                                                              (action/form-submit-state 
                                                                {:label (text/consenter-text :select.encounter.ok.label)
                                                                 :required {:type "inform" 
                                                                            :title (text/encounter-text :select.encounter.required.title)
                                                                            :message  (text/encounter-text :select.encounter.required.message)}})))
                                             items)))))
    (respond/forbidden-view ctx)))

(as-method view-select-encounter endpoint/endpoints "get-view-select-encounter")

;; Register API Service To Select Specific Consenter
(defprocess api-select-encounter
  "Selects the consenter to be working with."
  [ctx]
  (if (auth/is-authenticated?)
    (let [encounter (:body-params ctx)]
      (state/set-encounter encounter)
      (respond/with-actions {:encounter encounter :view-url "/view/select/protocol" :reset false} "setEncounter" "changeView"))
    (respond/forbidden-view ctx)))

(as-method api-select-encounter endpoint/endpoints "post-api-select-encounter")
