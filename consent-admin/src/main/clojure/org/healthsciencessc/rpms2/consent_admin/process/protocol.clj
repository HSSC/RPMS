;; Provides the configuration of the protocol managemant UIs.
(ns org.healthsciencessc.rpms2.consent-admin.process.protocol
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
            
            [org.healthsciencessc.rpms2.consent-domain.roles :as roles]
            [org.healthsciencessc.rpms2.consent-domain.lookup :as lookup]
            [org.healthsciencessc.rpms2.consent-domain.tenancy :as tenancy]
            
            [ring.util.response :as rutil]
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))

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

;; Register View Protocols Process
(defprocess view-protocols
  "Generates a view that shows all of the protocols available within a location."
  [ctx]
  (if-let [location-id (lookup/get-location-in-query ctx)]
    (let [user (security/current-user ctx)
          location-role (first (roles/protocol-designer-mappings user :location {:id location-id}))]
      (if location-role
        (let [protocols (services/get-protocols location-id)
              location (:location location-role)]
          (if (meta protocols)
            (error/view-service-error protocols)
            (layout/render ctx (render-label location " List")
                           (container/scrollbox 
                             (list/selectlist {:action :.detail-action}
                                              (for [protocol protocols]
                                                {:label (:name protocol) :data protocol})))
                           (actions/actions 
                             (actions/push-action 
                               {:url "/view/protocol" :params {:location location-id :protocol :selected#id}
                                :label "Details/Edit" :classes :.detail-action 
                                :verify (actions/gen-verify-a-selected (render-label location))})
                             (actions/push-action 
                               {:url "/view/protocol/new" :params {:location location-id}
                                :label "New"})
                             (actions/back-action)))))
        (ajax/forbidden)))
    (layout/render-error ctx {:message "A location is required."})))

(as-method view-protocols endpoint/endpoints "get-view-protocols")

;; Register View New Protocol Process
(defprocess view-protocol
  "Generates a view that shows the details of a specific protocol and allows you to edit those details."
  [ctx]
  (if-let [protocol-id (lookup/get-protocol-in-query ctx)]
    (let [protocol (services/get-protocol protocol-id)]
      (if (services/service-error? protocol)
        (error/view-service-error protocol)
        (let[location (:location protocol)
             location-id (:id location)]
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
                           (actions/back-action))))))
    (layout/render-error ctx {:message "A protocol is required."})))

(as-method view-protocol endpoint/endpoints "get-view-protocol")

;; Register View New Protocol Process
(defprocess view-protocol-new
  "Generates a view that allows you to create a new protocol."
  [ctx]
  (let [user (security/current-user ctx)
        location-id (lookup/get-location-in-query ctx)
        location-role (first (roles/protocol-designer-mappings user :location {:id location-id}))
        location (:location location-role)]
    (if location-role
      (layout/render ctx (str "Create " (render-label location))
                     (container/scrollbox (form/dataform (form/render-fields {} fields {:required true})))
                     (actions/actions 
                       (actions/ajax-action 
                         {:method :put :url "/api/protocol" :params {:location location-id}
                          :label "Create" :action-on-success ".back-action" :include-data :true})
                       (actions/back-action)))
      (ajax/forbidden))))

(as-method view-protocol-new endpoint/endpoints "get-view-protocol-new")

;; Register Create Protocol Process
(defprocess create
  [ctx]
    (if-let [location-id (lookup/get-location-in-query ctx)]
      (let [user (security/current-user ctx)
            location-role (first (roles/protocol-designer-mappings user :location {:id location-id}))]
        (if location-role
          (let [body (:body-params ctx)
                location (:location location-role)
                organization (:organization location-role)
                protocol (assoc body :organization organization :location location)
                resp (services/add-protocol location-id protocol)]
            (if (services/service-error? resp)
              (ajax/save-failed (meta resp))
              (ajax/success resp)))
          (ajax/forbidden)))
    (ajax/error {:message "A location is required."})))

(as-method create endpoint/endpoints "put-api-protocol")

;; Register Update Protocol Process
(defprocess update
  [ctx]
  (let [user (security/current-user ctx)
        location-id (lookup/get-location-in-query ctx)
        location-role (first (roles/protocol-designer-mappings user :location {:id location-id}))]
    (if location-role
      (let [body (:body-params ctx)
            protocol-id (lookup/get-protocol-in-query ctx)
            resp (services/update-protocol protocol-id body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method update endpoint/endpoints "post-api-protocol")

;; Register Update Protocol Process
(defprocess delete
  [ctx]
  (let [user (security/current-user ctx)
        location-id (lookup/get-location-in-query ctx)
        location-role (first (roles/protocol-designer-mappings user :location {:id location-id}))]
    (if location-role
      (let [protocol-id (lookup/get-protocol-in-query ctx)
            resp (services/delete-protocol protocol-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method delete endpoint/endpoints "delete-api-protocol")


