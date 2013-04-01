(ns org.healthsciencessc.consent.collector.process.search-consenter
  (:refer-clojure :exclude [root])
  (:require [org.healthsciencessc.consent.client.core :as services]
            [org.healthsciencessc.consent.collector.process.authorize :as auth]
            [org.healthsciencessc.consent.collector.respond :as respond]
            [org.healthsciencessc.consent.collector.state :as state]
            [org.healthsciencessc.consent.collector.text :as text]
            [org.healthsciencessc.consent.collector.ui.action :as action]
            [org.healthsciencessc.consent.collector.ui.form :as form]
            [org.healthsciencessc.consent.collector.ui.grid :as grid]
            [org.healthsciencessc.consent.collector.ui.layout :as layout]
            [pliant.webpoint.request :as endpoint]
            [clojure.string :as string])
  (:use     [pliant.process :only [defprocess as-method]]))


(def fields [{:name :first-name :type :text :label (text/text :person.firstname.label) :autofocus true}
             {:name :last-name :type :text :label (text/text :person.lastname.label)}
             {:name :consenter-id :type :text :label (text/text :consenter.id.label)}
             {:name :dob :type :date :label (text/text :person.dob.label)}
             {:name :zipcode :type :text :label (text/text :person.zipcode.label)}])


;; Register The Search Consenter View
(defprocess view-search-consenter
  "Creates a view for setting search criteria for consenters."
  [ctx]
  (state/reset-consent-session)
  (if (auth/is-authenticated?)
    (layout/render-page ctx {:title (text/consenter-text :search.consenter.title) :pageid "SearchConsenter"
                        :header-left (action/next-view {:label (text/location-text :action.change-location.label) 
                                                        :url "/view/select/location" :inline true})} 
                   (form/dataform {:method :post :url "/api/search/consenter"} 
                                  (form/render-fields {:fields {:consenter-id {:label (text/consenter-text :consenter.id.label)}}} fields (or (:query-params ctx) {}))
                                  (grid/gridify {}
                                                (action/form-submit {:label (text/consenter-text :search.consenter.search.label)})
                                                (action/next-view {:url "/view/create/consenter" :method :get
                                                     :label (text/consenter-text :search.consenter.create.label)}))))
    (respond/forbidden-view ctx)))

(as-method view-search-consenter endpoint/endpoints "get-view-search-consenter")


;; Register API Service To Search For Consenters
(defprocess api-search-consenter
  "Performs the consenter search "
  [ctx]
  (if (auth/is-authenticated?)
    (let [parms (:body-params ctx)
          valid-parms (into {} (filter (fn [[k v]] (not (string/blank? v))) parms))]
      (if (> (count valid-parms) 0)
        (let [results (services/find-consenters valid-parms)]
          (if (map? results)
            (respond/with-actions {:title (text/consenter-text :search.consenter.notfound.title)
                                   :message (text/consenter-text :search.consenter.notfound.message)} "inform")
            (do
              (state/flash-put! :consenter-search {:results results :criteria valid-parms})
              (respond/reset-view "/view/select/consenter"))))
        (respond/with-error (text/consenter-text :search.consenter.message.notvalid))))
    (respond/forbidden-view ctx)))

(as-method api-search-consenter endpoint/endpoints "post-api-search-consenter")

