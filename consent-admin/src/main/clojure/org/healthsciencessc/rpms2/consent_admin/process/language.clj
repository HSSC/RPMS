;; Provides the configuration of the protocol managemant UIs.
(ns org.healthsciencessc.rpms2.consent-admin.process.language
  (:require [org.healthsciencessc.rpms2.consent-admin.ajax :as ajax]
            [org.healthsciencessc.rpms2.consent-admin.error :as error]
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
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
            
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))

(def fields [{:name :name :label "Name" :required true}
             {:name :code :label "ANSI Language Code" :required true}])

(def type-name types/language)
(def type-label "Language")
(def type-path "language")
(def type-kw (keyword type-name))

;; Register View Languages Process
(defprocess view-languages
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (or (roles/can-design-org-id? user org-id) (roles/can-admin-org-id? user org-id))
      (let [nodes (services/get-languages org-id)
            protocol-version-id (lookup/get-protocol-version-in-query ctx)
            assign-to-org-id (lookup/get-organization-in-query ctx)]
        (if (meta nodes)
          (error/view-service-error nodes)
          (layout/render ctx (str type-label "s")
                         (container/scrollbox 
                           (list/selectlist {:action :.detail-action}
                                            (for [n nodes]
                                              {:label (:name n) :data (select-keys n [:id])})))
                         (actions/actions 
                           (cond
                             protocol-version-id
                               (actions/assign-action 
                                 {:url (str "/api/" type-path "/assign") 
                                  :params {:organization org-id type-kw :selected#id :protocol-version protocol-version-id}
                                  :verify (actions/gen-verify-a-selected "Language")})
                             assign-to-org-id
                               (actions/assign-action 
                                 {:url (str "/api/" type-path "/assign/organization") 
                                  :params {:organization assign-to-org-id type-kw :selected#id}
                                  :verify (actions/gen-verify-a-selected "Language")}))
                           (actions/details-action 
                             {:url (str "/view/" type-path) :params {:organization org-id type-kw :selected#id}
                              :verify (actions/gen-verify-a-selected "Language")})
                           (actions/new-action 
                             {:url (str "/view/" type-path "/new") :params {:organization org-id}})
                           (actions/back-action)))))
      (ajax/forbidden))))
    
(as-method view-languages endpoint/endpoints "get-view-languages")
(as-method view-languages endpoint/endpoints "get-view-protocol-version-language-add")

;; Register View Language Process
(defprocess view-language
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (if-let [node-id (lookup/get-language-in-query ctx)]
        (let [n (services/get-language node-id)
              editable (common/owned-by-user-org n)]
          (if (meta n)
            (error/view-service-error n)
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
        (layout/render-error ctx {:message "An language type is required."}))
      (ajax/forbidden))))

(as-method view-language endpoint/endpoints "get-view-language")

;; Register View New Language Process
(defprocess view-language-new
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (layout/render ctx (str "Create " type-label)
                     (container/scrollbox 
                       (form/dataform 
                         (form/render-fields {} fields )))
                     (actions/actions 
                       (actions/create-action 
                         {:url (str "/api/" type-path) :params {:organization org-id}})
                       (actions/back-action)))
      (ajax/forbidden))))

(as-method view-language-new endpoint/endpoints "get-view-language-new")

;; Register Assign Language To Protocol Version Process
(defprocess assign-to-protocol-version
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (let [language-id (lookup/get-language-in-query ctx)
            protocol-version-id (lookup/get-protocol-version-in-query ctx)
            resp (services/assign-language-to-protocol-version language-id protocol-version-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method assign-to-protocol-version endpoint/endpoints "post-api-language-assign")

;; Register Assign Language To Organization Process
(defprocess assign-to-organization
  [ctx]
  (let [user (security/current-user ctx)
        org-id (lookup/get-organization-in-query ctx)]
    (if (roles/can-admin-org-id? user org-id)
      (let [language-id (lookup/get-language-in-query ctx)
            resp (services/assign-language-to-organization language-id org-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method assign-to-organization endpoint/endpoints "post-api-language-assign-organization")

;; Register Create Language Process
(defprocess create
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (let [body (assoc (:body-params ctx) :organization {:id org-id})
            resp (services/add-language org-id body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method create endpoint/endpoints "put-api-language")

;; Register Update Language Process
(defprocess update
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (let [body (:body-params ctx)
            language-id (lookup/get-language-in-query ctx)
            resp (services/update-language language-id body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method update endpoint/endpoints "post-api-language")

;; Register Update Language Process
(defprocess delete
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (let [language-id (lookup/get-language-in-query ctx)
            resp (services/delete-language language-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method delete endpoint/endpoints "delete-api-language")

