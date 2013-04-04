(ns org.healthsciencessc.consent.collector.process.select-consenter
  (:refer-clojure :exclude [root])
  (:require [org.healthsciencessc.consent.collector.common :as common]
            [org.healthsciencessc.consent.collector.process.authorize :as auth]
            [org.healthsciencessc.consent.collector.respond :as respond]
            [org.healthsciencessc.consent.collector.state :as state]
            [org.healthsciencessc.consent.collector.text :as text]
            [org.healthsciencessc.consent.collector.ui.action :as action]
            [org.healthsciencessc.consent.collector.ui.content :as uicontent]
            [org.healthsciencessc.consent.collector.ui.form :as form]
            [org.healthsciencessc.consent.collector.ui.grid :as grid]
            [org.healthsciencessc.consent.collector.ui.layout :as layout]
            [pliant.webpoint.request :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))


(def fields [{:name :id :type :hidden :disabled true}
             {:name :first-name :type :text :label (text/text :person.firstname.label) :disabled true}
             {:name :last-name :type :text :label (text/text :person.lastname.label) :disabled true}
             {:name :consenter-id :type :text :label (text/text :consenter.id.label) :disabled true}
             {:name :dob :type :date :label (text/text :person.dob.label) :disabled true}
             {:name :zipcode :type :text :label (text/text :person.zipcode.label) :disabled true}])


;; Register The Select Consenter View
(defprocess view-select-consenter
  "Creates a view of the search results to select a consenter from."
  [ctx]
  (if (auth/is-authenticated?)
    (let [search (state/flash-get :consenter-search)
          consenters (:results search)
          criteria (:criteria search)
          ziplabel (text/text :person.zipcode.label)
          items (for [c consenters] {:data c :label (common/formal-name c) :sublabel (str ziplabel ": " (:zipcode c))})]
      (layout/render-page ctx {:title (text/consenter-text :select.consenter.title) :pageid "SelectConsenterResults"}
                     (uicontent/datalistview {:autoselect :first}
                                             (form/dataform {:method :post :url "/api/select/consenter"}
                                                            (form/render-fields {:fields {:consenter-id {:label (text/consenter-text :consenter.id.label)}}}
                                                                                fields {})
                                                            (grid/gridify {}
                                                              (action/next-view {:url "/view/search/consenter" :method :get
                                                                                 :parameters criteria
                                                                                 :label (text/consenter-text :select.consenter.back.label)})
                                                              (action/form-submit-state 
                                                                {:label (text/consenter-text :select.consenter.ok.label)
                                                                 :required {:type "inform" 
                                                                            :title (text/consenter-text :select.consenter.required.title)
                                                                            :message  (text/consenter-text :select.consenter.required.message)}})))
                                             items)))
    (respond/forbidden-view ctx)))

(as-method view-select-consenter endpoint/endpoints "get-view-select-consenter")

;; Register API Service To Select Specific Consenter
(defprocess api-select-consenter
  "Selects the consenter to be working with."
  [ctx]
  (if (auth/is-authenticated?)
    (let [consenter (:body-params ctx)
          url (if (seq (:encounters consenter)) "/view/select/encounter"  "/view/create/encounter")]
      (state/set-consenter consenter)
      (respond/with-actions {:consenter consenter :view-url url :reset false} "setConsenter" "changeView"))
    (respond/forbidden-view ctx)))

(as-method api-select-consenter endpoint/endpoints "post-api-select-consenter")
