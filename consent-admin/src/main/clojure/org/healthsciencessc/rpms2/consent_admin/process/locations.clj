(ns org.healthsciencessc.rpms2.consent-admin.process.locations
  (:require [org.healthsciencessc.rpms2.consent-admin.ajax :as ajax]
            [org.healthsciencessc.rpms2.consent-admin.security :as security]
            [org.healthsciencessc.rpms2.consent-admin.services :as services]
            
            [org.healthsciencessc.rpms2.consent-admin.process.common :as common]
            
            [org.healthsciencessc.rpms2.consent-admin.ui.container :as container]
            [org.healthsciencessc.rpms2.consent-admin.ui.layout :as layout]
            [org.healthsciencessc.rpms2.consent-admin.ui.actions :as actions]
            [org.healthsciencessc.rpms2.consent-admin.ui.list :as list]
            [org.healthsciencessc.rpms2.consent-admin.ui.form :as form]
            
            [org.healthsciencessc.rpms2.consent-domain.tenancy :as tenancy]
            [org.healthsciencessc.rpms2.consent-domain.lookup :as lookup]
            [org.healthsciencessc.rpms2.consent-domain.runnable :as runnable]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
            
            [ring.util.response :as rutil]
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))

(def fields [{:name :name :label "Name"}
             {:name :code :label "Code"}
             {:name :protocol-label :label "Protocol Label"}
             {:name :consenter-label :label "Consenter Label"}])

(def type-name types/location)
(def type-label "Location")
(def type-path "location")
(def type-kw (keyword type-name))

(defn- render-label
  [& addons]
  (let [org (security/current-org)
        label (tenancy/label-for-location nil org)]
    (str label (apply str addons))))

;; Register View Locations Process
(defprocess view-locations
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (runnable/can-admin-org-id user org-id)
      (let [nodes (services/get-locations)]
        (if (meta nodes)
          (rutil/not-found (:message (meta nodes)))
          (layout/render ctx (render-label "s")
                         (container/scrollbox 
                           (list/selectlist {:action :.detail-action}
                                            (for [n (sort-by :name nodes)]
                                              {:label (:name n) :data (select-keys n [:id])})))
                         (actions/actions 
                          (actions/details-action 
                               {:url (str "/view/" type-path) :params {:organization org-id type-kw :selected#id}
                                :verify (actions/gen-verify-a-selected (render-label))})
                           (actions/new-action 
                             {:url (str "/view/" type-path "/new") :params {:organization org-id}})
                           (actions/back-action)))))
      (ajax/forbidden))))
    
(as-method view-locations endpoint/endpoints "get-view-locations")

;; Register View Location Process
(defprocess view-location
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (runnable/can-admin-org-id user org-id)
      (if-let [node-id (lookup/get-location-in-query ctx)]
        (let [n (services/get-location node-id)
              editable (common/owned-by-user-org n)]
          (if (meta n)
            (rutil/not-found (:message (meta n)))
            (layout/render ctx (render-label ": " (:name n))
                           (container/scrollbox 
                             (form/dataform 
                               (form/render-fields {:editable editable} fields n)))
                           (actions/actions
                             (if editable
                               (list
                                 (actions/save-action 
                                   {:url (str "/api/" type-path) :params {type-kw node-id}})
                                 (actions/delete-action 
                                   {:url (str "/api/" type-path) :params {type-kw node-id}})))
                             (actions/back-action)))))
        ;; Handle Error
        (layout/render-error ctx {:message "An location type is required."}))
      (ajax/forbidden))))

(as-method view-location endpoint/endpoints "get-view-location")

;; Register View New Location Process
(defprocess view-location-new
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (runnable/can-admin-org-id user org-id)
      (layout/render ctx (str "Create " (render-label))
                     (container/scrollbox 
                       (form/dataform 
                         (form/render-fields {} fields )))
                     (actions/actions 
                       (actions/create-action 
                         {:url (str "/api/" type-path) :params {:organization org-id}})
                       (actions/back-action)))
      (ajax/forbidden))))

(as-method view-location-new endpoint/endpoints "get-view-location-new")


;; Register Create Location Process
(defprocess create
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (runnable/can-admin-org-id user org-id)
      (let [body (assoc (:body-params ctx) :organization {:id org-id})
            resp (services/add-location body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method create endpoint/endpoints "put-api-location")

;; Register Update Location Process
(defprocess update
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (runnable/can-admin-org-id user org-id)
      (let [body (:body-params ctx)
            location-id (lookup/get-location-in-query ctx)
            resp (services/update-location location-id body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method update endpoint/endpoints "post-api-location")

;; Register Update Location Process
(defprocess delete
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (runnable/can-admin-org-id user org-id)
      (let [location-id (lookup/get-location-in-query ctx)
            resp (services/delete-location location-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method delete endpoint/endpoints "delete-api-location")


