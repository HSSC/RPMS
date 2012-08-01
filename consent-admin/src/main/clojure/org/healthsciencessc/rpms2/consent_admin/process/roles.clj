(ns org.healthsciencessc.rpms2.consent-admin.process.roles
  (:require [org.healthsciencessc.rpms2.consent-admin.ajax :as ajax]
            [org.healthsciencessc.rpms2.consent-admin.security :as security]
            [org.healthsciencessc.rpms2.consent-client.core :as services]
            
            [org.healthsciencessc.rpms2.consent-admin.process.common :as common]
            
            [org.healthsciencessc.rpms2.consent-admin.ui.actions :as actions]
            [org.healthsciencessc.rpms2.consent-admin.ui.container :as container]
            [org.healthsciencessc.rpms2.consent-admin.ui.form :as form]
            [org.healthsciencessc.rpms2.consent-admin.ui.layout :as layout]
            [org.healthsciencessc.rpms2.consent-admin.ui.list :as list]
            
            [org.healthsciencessc.rpms2.consent-domain.lookup :as lookup]
            [org.healthsciencessc.rpms2.consent-domain.roles :as roles]
            [org.healthsciencessc.rpms2.consent-domain.tenancy :as tenancy]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
        
            [ring.util.response :as rutil]
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))

(def fields [{:name :name :label "Name"}
             {:name :code :label "Code"}
             {:name :requires-location :label "Requires Location" :type :checkbox}])

(def type-name types/role)
(def type-label "Role")
(def type-path "role")
(def type-kw (keyword type-name))
 
;; Register View Roles Process
(defprocess view-roles
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-admin-org-id? user org-id)
      (let [nodes (services/get-roles)]
        (if (meta nodes)
          (rutil/not-found (:message (meta nodes)))
          (layout/render ctx (str type-label "s")
                         (container/scrollbox 
                           (list/selectlist {:action :.detail-action}
                                            (for [n (sort-by :name nodes)]
                                              {:label (:name n) :data (select-keys n [:id])})))
                         (actions/actions 
                          (actions/details-action 
                               {:url (str "/view/" type-path) :params {:organization org-id type-kw :selected#id}
                                :verify (actions/gen-verify-a-selected type-label)})
                           (actions/new-action 
                             {:url (str "/view/" type-path "/new") :params {:organization org-id}})
                           (actions/back-action)))))
      (ajax/forbidden))))
    
(as-method view-roles endpoint/endpoints "get-view-roles")

;; Register View Role Process
(defprocess view-role
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-admin-org-id? user org-id)
      (if-let [node-id (lookup/get-role-in-query ctx)]
        (let [n (services/get-role node-id)
              editable (common/owned-by-user-org n)]
          (if (meta n)
            (rutil/not-found (:message (meta n)))
            (layout/render ctx (str type-label ": " (:name n))
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
        (layout/render-error ctx {:message "An role is required."}))
      (ajax/forbidden))))

(as-method view-role endpoint/endpoints "get-view-role")

;; Register View New Role Process
(defprocess view-role-new
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-admin-org-id? user org-id)
      (layout/render ctx (str "Create " type-label)
                     (container/scrollbox 
                       (form/dataform 
                         (form/render-fields {} fields)))
                     (actions/actions 
                       (actions/create-action 
                         {:url (str "/api/" type-path) :params {:organization org-id}})
                       (actions/back-action)))
      (ajax/forbidden))))

(as-method view-role-new endpoint/endpoints "get-view-role-new")

;; Register Create Role Process
(defprocess create
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-admin-org-id? user org-id)
      (let [body (assoc (:body-params ctx) :organization {:id org-id})
            body (common/find-and-replace-truths body [:requires-location] true "true")
            resp (services/add-role org-id body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method create endpoint/endpoints "put-api-role")

;; Register Update Role Process
(defprocess update
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-admin-org-id? user org-id)
      (let [body (:body-params ctx)
            body (common/find-and-replace-truths body [:requires-location] true "true")
            role-id (lookup/get-role-in-query ctx)
            resp (services/update-role role-id body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method update endpoint/endpoints "post-api-role")

;; Register Update Role Process
(defprocess delete
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-admin-org-id? user org-id)
      (let [role-id (lookup/get-role-in-query ctx)
            resp (services/delete-role role-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method delete endpoint/endpoints "delete-api-role")
