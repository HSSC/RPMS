;; Provides the configuration of the protocol managemant UIs.
(ns org.healthsciencessc.rpms2.consent-admin.process.protocol
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-admin.security :as security]
            [org.healthsciencessc.rpms2.consent-admin.ui.layout :as layout]
            [org.healthsciencessc.rpms2.consent-domain.roles :as roles]
            [org.healthsciencessc.rpms2.consent-domain.runnable :as runnable]
            [org.healthsciencessc.rpms2.consent-admin.services :as services]
            [org.healthsciencessc.rpms2.consent-admin.process.common :as common]
            [org.healthsciencessc.rpms2.consent-admin.ui.container :as container]
            [org.healthsciencessc.rpms2.consent-admin.ui.actions :as actions]
            [org.healthsciencessc.rpms2.consent-admin.ui.selectlist :as selectlist]
            [org.healthsciencessc.rpms2.consent-admin.ui.form :as form]
            [org.healthsciencessc.rpms2.consent-admin.ajax :as ajax]
            [org.healthsciencessc.rpms2.consent-domain.lookup :as lookup]
            [org.healthsciencessc.rpms2.consent-domain.tenancy :as tenancy]
            [hiccup.core :as hcup]
            [clojure.data.json :as json]
            [ring.util.response :as rutil])
  (:use [clojure.tools.logging :only (info error)])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def fields [{:name :name :label "Name"}
             {:name :description :label "Description"}
             {:name :protocol-id :label "External ID"}
             {:name :code :label "Code"}
             {:name :required :label "Required" :type :checkbox}
             {:name :select-by-default :label "Selected By Default" :type :checkbox}])

(defn- render-label
  "Helper function to generate labels using the appropriate text for protocols."
  [location & addons]
  (let [user (security/current-user)
        org (:organization user)
        label (tenancy/label-for-protocol location org)]
    (str label (apply str addons))))

(defn view-protocol-location
  "Generates a view that shows all of the protocols available within a location."
  [ctx]
  (if-let [location-id (get-in ctx [:query-params :location])]
    (let [protocols (services/get-protocols location-id)
          location (if (first protocols) (:location (first protocols)) (services/get-location location-id))]
      (if (meta protocols)
        (rutil/not-found (:message (meta protocols)))
        (layout/render ctx (render-label location " List")
          (container/scrollbox 
            (selectlist/selectlist {:action :.detail-action}
              (for [protocol protocols]
                {:label (:name protocol) :data protocol})))
          (actions/actions 
            (actions/push-action 
                           {:url "/view/protocol" :params {:location location-id :protocol :selected#id}
                            :label "Details/Edit" :classes :.detail-action :verify (actions/gen-verify-a-selected "Protocol")})
            (actions/push-action 
                           {:url "/view/protocol/new" :params {:location location-id}
                            :label "New"})
           (actions/back-action)))))
    ;; Handle Error
    (layout/render-error ctx {:message "A location is required."})))

(defn view-protocol
  "Generates a view that shows the details of a specific protocol and allows you to edit those details."
  [ctx]
  (if-let [protocol-id (get-in ctx [:query-params :protocol])]
    (let [protocol (services/get-protocol protocol-id)
          location (:location protocol)
          location-id (:id location)]
      (if (not= location-id (get-in protocol [:location :id]))
        (layout/render-error ctx {:message "Location provided must match the location of the protocol requested."})
        (layout/render ctx (render-label location ": " (:name protocol))
                       (container/scrollbox (form/dataform (form/render-fields {} fields protocol)))
                       (actions/actions
                         (actions/push-action 
                           {:url "/view/protocol/versions" :params {:protocol protocol-id} :label "Versions"})
                         (actions/ajax-action 
                           {:method :post :url "/api/protocol" :params {:protocol protocol-id :location location-id}
                            :label "Save" :include-data :true})
                         (actions/ajax-action 
                           {:method :delete :url "/api/protocol" :params {:protocol protocol-id :location location-id}
                            :label "Delete" :action-on-success ".back-action"})
                         (actions/back-action)))))
    ;; Handle Error
    (layout/render-error ctx {:message "A location and protocol are required."})))

(defn view-protocol-new
  "Generates a view that allows you to create a new protocol."
  [ctx]
  (let [location-id (get-in ctx [:query-params :location])
        location (services/get-location location-id)]
    (layout/render ctx (str "Create " (render-label location))
                   (container/scrollbox (form/dataform (form/render-fields {} fields {:required true})))
                   (actions/actions 
                     (actions/ajax-action 
                       {:method :put :url "/api/protocol" :params {:location location-id}
                        :label "Create" :action-on-success ".back-action" :include-data :true})
                     (actions/back-action)))))

(defn api-add-protocol
  "Adds a new protocol to a location."
  [ctx]
  (if-let [location-id (get-in ctx [:query-params :location])]
    (let [body (select-keys (:body-params ctx) (map :name fields))
          body (common/find-and-replace-truths body [:required :select-by-default] "true")
          user (security/current-user)
          location-role (first (roles/protocol-designer-mappings user :location {:id location-id}))
          location (:location location-role)
          protocol (assoc body :organization (:organization user) :location location)
          resp (services/add-protocol protocol)]
      ;; Handle Error or Success
      (if (services/service-error? resp)
        (ajax/save-failed (meta resp))
        (ajax/success resp)))
    ;; Handle Bad Request
    (ajax/error {:message "A location is required."})))

(defn api-update-protocol
  "Updates a protocol."
  [ctx]
  (if-let [protocol-id (get-in ctx [:query-params :protocol])]
    (let [body (select-keys (:body-params ctx) (map :name fields))
          body (common/find-and-replace-truths body [:required :select-by-default] "true")
          resp (services/update-protocol protocol-id body)]
      ;; Handle Error or Success
      (if (services/service-error? resp)
        (ajax/save-failed (meta resp))
        (ajax/success resp)))
    ;; Handle Bad Request
    (ajax/error {:message "A protocol is required."})))

(defn api-delete-protocol
  "Deletes a protocol."
  [ctx]
  (if-let [protocol-id (get-in ctx [:query-params :protocol])]
    (let [resp (services/delete-protocol protocol-id)]
      ;; Handle Error or Success
      (if (services/service-error? resp)
        (ajax/error (meta resp))
        (ajax/success resp)))
    ;; Handle Bad Request
    (ajax/error {:message "A protocol is required."})))

(def process-defns
  [
   ;; Generates the view for the protocol list in a location.
   {:name "get-view-protocol-location"
    :runnable-fn (runnable/gen-designer-location-check security/current-user lookup/get-location-in-query)
    :run-fn view-protocol-location
    :run-if-false ajax/forbidden}
   
   ;; Generates the view for a specific protocol.
   {:name "get-view-protocol"
    :runnable-fn (runnable/gen-designer-location-check security/current-user lookup/get-location-in-query)
    :run-fn view-protocol
    :run-if-false ajax/forbidden}
   
   ;; Generates the view for creating a protocol.
   {:name "get-view-protocol-new"
    :runnable-fn (runnable/gen-designer-location-check security/current-user lookup/get-location-in-query)
    :run-fn view-protocol-new
    :run-if-false ajax/forbidden}
   
   ;; Generates the api service for creating a protocol.
   {:name "put-api-protocol"
    :runnable-fn (runnable/gen-designer-location-check security/current-user lookup/get-location-in-query)
    :run-fn api-add-protocol
    :run-if-false ajax/forbidden}
   
   ;; Generates the api service for creating a protocol.
   {:name "post-api-protocol"
    :runnable-fn (runnable/gen-designer-location-check security/current-user lookup/get-location-in-query)
    :run-fn api-update-protocol
    :run-if-false ajax/forbidden}
   
   ;; Generates the api service for creating a protocol.
   {:name "delete-api-protocol"
    :runnable-fn (runnable/gen-designer-location-check security/current-user lookup/get-location-in-query)
    :run-fn api-delete-protocol
    :run-if-false ajax/forbidden}
   ])

(process/register-processes (map #(DefaultProcess/create %) process-defns))
