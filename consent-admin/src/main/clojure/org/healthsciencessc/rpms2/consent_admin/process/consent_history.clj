;; Provides the configuration of the protocol managemant UIs.
(ns org.healthsciencessc.rpms2.consent-admin.process.consent-history
  (:require [org.healthsciencessc.rpms2.consent-admin.ajax :as ajax]
            [org.healthsciencessc.rpms2.consent-admin.error :as error]
            [org.healthsciencessc.rpms2.consent-admin.security :as security]
            [org.healthsciencessc.rpms2.consent-admin.services :as services]
            [org.healthsciencessc.rpms2.consent-admin.process.common :as common]
            
            [org.healthsciencessc.rpms2.consent-admin.ui.actions :as actions]
            [org.healthsciencessc.rpms2.consent-admin.ui.container :as container]
            [org.healthsciencessc.rpms2.consent-admin.ui.form :as form]
            [org.healthsciencessc.rpms2.consent-admin.ui.layout :as layout]
            [org.healthsciencessc.rpms2.consent-admin.ui.list :as list]
            
            [org.healthsciencessc.rpms2.consent-domain.runnable :as runnable]
            [org.healthsciencessc.rpms2.consent-domain.tenancy :as tenancy]
            
            [org.healthsciencessc.rpms2.process-engine.core :as process]
            [ring.util.response :as rutil])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def fields [{:name :first-name :label "First Name"}
             {:name :middle-name :label "Middle Name"}
             {:name :last-name :label "Last Name"}
             {:name :gender :label "Gender" :type :singleselect :items [{:label "Unknown" :data ""}
                                                                        {:label "Female" :data "F"}
                                                                        {:label "Male" :data "M"}]}
             {:name :dob :label "Date Of Birth" :type :date}
             {:name :zipcode :label "Zip Code"}])

(defn view-consent-history
  [ctx]
  (layout/render 
    ctx "Consent History - Search"
    (container/scrollbox 
      (form/dataform 
        (form/render-fields {} fields {})))
    (actions/actions
      (actions/pushform-action
        {:url "/view/consent/history/consenters" :params {} :label "Search"})
      (actions/back-action))))

(defn consenter-name
  [consenter]
  (str (:last-name consenter) ", " (:first-name consenter) " " (:middle-name consenter)))

(defn- no-results?
  [resp]
  (= (:status (meta resp)) 404))

(defn view-consent-history-consenters
  [ctx]
  (let [consenters (services/find-consenters (:query-params ctx))]
    (cond
      (no-results? consenters)
        (error/view-not-found {:message "No records found that match the entered criteria."})
      (meta consenters)
        (error/view-failure {:message "Unable to execute the search."} 500)
      :else
        (layout/render 
          ctx "Consent History - Search Results"
          (container/scrollbox 
            (list/selectlist {:action :.detail-action}
                             (for [n consenters]
                               {:label (consenter-name n) :data (select-keys n [:id])})))
          (actions/actions 
            (actions/details-action 
              {:url "/view/consent/history/consents" 
               :params {:consenter :selected#id}
               :verify (actions/gen-verify-a-selected "Consenter")})
            (actions/back-action))))))

(defn view-consent-history-consents
  [ctx]
  (let [consenter-id (get-in ctx [:query-params :consenter])
        consents (services/get-consenter-consents consenter-id)]
    (if (meta consents)
      (rutil/not-found (:message (meta consents)))
      (layout/render ctx "Consent History - Consents"
                     (container/scrollbox 
                       (list/labelled-list {:columns 3 :title (tenancy/label-for-consenter nil (:organization consents))}
                                           [{:label "Consenter ID:" :data (:consenter-id consents)} {} {}
                                            {:label "Last Name:" :data (:last-name consents)}
                                            {:label "First Name:" :data (:first-name consents)}
                                            {:label "Middle Name:" :data (:middle-name consents)}
                                            {:label "Gender:" :data (:gender consents)}
                                            {:label "Date Of Birth:" :data (:dob consents)}
                                            {:label "Zip Code:" :data (:zipcode consents)}])
                       (list/select-table {:headers ["Location" "Date" "Protocol ""Policy" "Consented"]}
                                          (flatten (for [encounter (:encounters consents)]
                                            (for [consent (:consents encounter)]
                                              {:data {:consenter consenter-id 
                                                      :location (get-in encounter [:location :id])
                                                      :protocol-version (get-in consent [:protocol-version :id])
                                                      :policy (get-in consent [:policy :id])
                                                      :encounter (:id encounter)
                                                      :consent (:id consent)}
                                               :labels [(get-in encounter [:location :name]) 
                                                       (:date encounter)
                                                       (str (get-in consent [:protocol-version :protocol :name])
                                                         " - Version " (get-in consent [:protocol-version :version]))
                                                       (get-in consent [:policy :name])
                                                       (:consented consent)]})))))
                     (actions/actions 
                       (actions/back-action))))))

(def process-defns
  [{:name "get-view-consent-history"
    :runnable-fn (runnable/gen-designer-org-check security/current-user common/lookup-organization)
    :run-fn view-consent-history
    :run-if-false ajax/forbidden}
   
   {:name "get-view-consent-history-consenters"
    :runnable-fn (runnable/gen-designer-org-check security/current-user common/lookup-organization)
    :run-fn view-consent-history-consenters
    :run-if-false ajax/forbidden}
   
   {:name "get-view-consent-history-consents"
    :runnable-fn (runnable/gen-designer-org-check security/current-user common/lookup-organization)
    :run-fn view-consent-history-consents
    :run-if-false ajax/forbidden}])

(process/register-processes (map #(DefaultProcess/create %) process-defns))
