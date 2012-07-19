;; Provides the configuration of the protocol managemant UIs.
(ns org.healthsciencessc.rpms2.consent-admin.process.policy-definition
  (:require [org.healthsciencessc.rpms2.consent-admin.ajax :as ajax]
            [org.healthsciencessc.rpms2.consent-admin.security :as security]
            [org.healthsciencessc.rpms2.consent-admin.services :as services]
            [org.healthsciencessc.rpms2.consent-admin.process.common :as common]
            
            [org.healthsciencessc.rpms2.consent-admin.ui.actions :as actions]
            [org.healthsciencessc.rpms2.consent-admin.ui.container :as container]
            [org.healthsciencessc.rpms2.consent-admin.ui.form :as form]
            [org.healthsciencessc.rpms2.consent-admin.ui.layout :as layout]
            [org.healthsciencessc.rpms2.consent-admin.ui.list :as list]
            
            [org.healthsciencessc.rpms2.consent-domain.lookup :as lookup]
            [org.healthsciencessc.rpms2.consent-domain.runnable :as runnable]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
            
            [ring.util.response :as rutil]
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))

(def fields [{:name :name :label "Name" :required true}
             {:name :description :label "Description"}
             {:name :code :label "Code"}])

(def type-name types/policy-definition)
(def type-label "Policy Definition")
(def type-path "policy-definition")
(def type-kw (keyword type-name))


;; Register View Policy Definitions Process
(defprocess view-policy-definitions
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (runnable/can-design-org-id user org-id)
      (let [nodes (services/get-policy-definitions org-id)]
        (if (meta nodes)
          (rutil/not-found (:message (meta nodes)))
          (layout/render ctx (str type-label "s")
                         (container/scrollbox 
                           (list/selectlist {:action :.detail-action}
                                            (for [n nodes]
                                              {:label (:name n) :data (select-keys n [:id])})))
                         (actions/actions 
                           (actions/details-action 
                             {:url (str "/view/" type-path) :params {:organization org-id type-kw :selected#id}
                              :verify (actions/gen-verify-a-selected "Policy Definition")})
                           (actions/new-action 
                             {:url (str "/view/" type-path "/new") :params {:organization org-id}})
                           (actions/back-action)))))
    (ajax/forbidden))))

(as-method view-policy-definitions endpoint/endpoints "get-view-policy-definitions")


;; Register View Policy Definition Process
(defprocess view-policy-definition
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (runnable/can-design-org-id user org-id)
      (if-let [node-id (lookup/get-policy-definition-in-query ctx)]
        (let [n (services/get-policy-definition node-id)
              org-id (get-in n [:organization :id])
              editable (= (get-in n [:organization :id]) (security/current-org-id))]
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
        (layout/render-error ctx {:message "An policy-definition type is required."}))
      (ajax/forbidden))))

(as-method view-policy-definition endpoint/endpoints "get-view-policy-definition")

;; Register View New Policy Definition Process
(defprocess view-policy-definition-new
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (runnable/can-design-org-id user org-id)
      (layout/render ctx (str "Create " type-label)
                     (container/scrollbox 
                       (form/dataform 
                         (form/render-fields {} fields )))
                     (actions/actions 
                       (actions/create-action 
                         {:url (str "/api/" type-path) :params {:organization org-id}})
                       (actions/back-action)))
      (ajax/forbidden))))

(as-method view-policy-definition-new endpoint/endpoints "get-view-policy-definition-new")

;; Register Create Policy Definition Process
(defprocess create
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (runnable/can-design-org-id user org-id)
      (let [body (assoc (:body-params ctx) :organization {:id org-id})
            resp (services/add-policy-definition body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method create endpoint/endpoints "put-api-policy-definition")

;; Register Update Policy Definition Process
(defprocess update
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (runnable/can-design-org-id user org-id)
      (let [body (:body-params ctx)
            policy-definition-id (lookup/get-policy-definition-in-query ctx)
            resp (services/update-policy-definition policy-definition-id body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method update endpoint/endpoints "post-api-policy-definition")

;; Register Update Policy Definition Process
(defprocess delete
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (runnable/can-design-org-id user org-id)
      (let [policy-definition-id (lookup/get-policy-definition-in-query ctx)
            resp (services/delete-policy-definition policy-definition-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method delete endpoint/endpoints "delete-api-policy-definition")

