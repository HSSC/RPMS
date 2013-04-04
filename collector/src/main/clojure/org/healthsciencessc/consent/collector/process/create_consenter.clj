(ns org.healthsciencessc.consent.collector.process.create-consenter
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
            [pliant.webpoint.request :as endpoint]
            [clojure.string :as string])
  (:use     [pliant.process :only [defprocess as-method]]))


(def fields [{:name :first-name :type :text :label (text/text :person.firstname.label) :contain true}
             {:name :middle-name :type :text :label (text/text :person.middlename.label) :contain true}
             {:name :last-name :type :text :label (text/text :person.lastname.label) :contain true}
             {:name :title :type :text :label (text/text :person.title.label) :contain true}
             {:name :suffix :type :text :label (text/text :person.suffix.label) :contain true}
             {:name :consenter-id :type :text :label (text/text :consenter.id.label) :contain true}
             {:name :gender :type :select-one :label (text/text :person.gender.label) :contain true
              :items [{:value :m :label (text/text :person.gender-male.label)}
                      {:value :f :label (text/text :person.gender-female.label)}
                      {:value :u :label (text/text :person.gender-unknown.label)}]}
             {:name :dob :type :date :label (text/text :person.dob.label) :contain true}
             {:name :zipcode :type :text :label (text/text :person.zipcode.label) :contain true}])

(def form-options {:method :put
                   :url "/api/consenter"})

;; Register The Select Consenter View
(defprocess view-create-consenter
  "Creates a view of locations to be selected."
  [ctx]
  (if (auth/is-authenticated?)
    (layout/render-page ctx {:title (text/consenter-text :create.consenter.title) :pageid "CreateConsenter"} 
                   (form/dataform form-options 
                                  (form/render-fields {:fields {:consenter-id {:label (text/consenter-text :consenter.id.label)}}}
                                                      fields {})
                                  (action/wrapper
                                    (action/form-submit {:label (text/text :action.create.label)}))))
    (respond/forbidden-view ctx)))

(as-method view-create-consenter endpoint/endpoints "get-view-create-consenter")


;; Register API Service To Persist The Consenter
(defprocess api-create-consenter
  "Performs the consenter search "
  [ctx]
  (if (auth/is-authenticated?)
    (let [{:keys [first-name last-name consenter-id gender dob zipcode] :as data} (:body-params ctx)]
      (cond
        (string/blank? first-name) (respond/with-error (text/text :create.consenter.firstname.required))
        (string/blank? last-name) (respond/with-error (text/text :create.consenter.lastname.required))
        (string/blank? consenter-id) (respond/with-error (text/consenter-text :create.consenter.consenterid.required))
        (string/blank? gender) (respond/with-error (text/text :create.consenter.gender.required))
        (string/blank? dob) (respond/with-error (text/text :create.consenter.dob.required))
        (string/blank? zipcode) (respond/with-error (text/text :create.consenter.zipcode.required))
        :else
        (let [location-id (:id (state/get-location))
              consenter (services/add-consenter location-id data)]
          (state/set-consenter consenter)
          (respond/with-actions {:consenter consenter :view-url "/view/create/encounter" :reset false} "setConsenter" "changeView"))))
    (respond/forbidden-view ctx)))

(as-method api-create-consenter endpoint/endpoints "put-api-consenter")
