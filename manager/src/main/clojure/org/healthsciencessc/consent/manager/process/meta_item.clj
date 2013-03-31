;; Provides the configuration of the protocol managemant UIs.
(ns org.healthsciencessc.consent.manager.process.meta-item
  (:require [org.healthsciencessc.consent.manager.ajax :as ajax]
            [org.healthsciencessc.consent.manager.error :as error]
            [org.healthsciencessc.consent.manager.security :as security]
            [org.healthsciencessc.consent.client.core :as services]
            [org.healthsciencessc.consent.manager.process.common :as common]
            
            [org.healthsciencessc.consent.manager.ui.actions :as actions]
            [org.healthsciencessc.consent.manager.ui.container :as container]
            [org.healthsciencessc.consent.manager.ui.form :as form]
            [org.healthsciencessc.consent.manager.ui.layout :as layout]
            [org.healthsciencessc.consent.manager.ui.list :as list]
            
            [org.healthsciencessc.consent.domain.lookup :as lookup]
            [org.healthsciencessc.consent.domain.roles :as roles]
            [org.healthsciencessc.consent.domain.types :as types]
            
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))

(def fields [{:name :name :label "Name" :required true}
             {:name :description :label "Description"}
             {:name :data-type :label "Data Type" :type :singleselect :items [{:label "String" :data "string"}
                                                                               {:label "Date" :data "date"}
                                                                               {:label "Number" :data "number"}
                                                                               {:label "Boolean" :data "boolean"}]}
             ;;{:name :choice-values :label "Choice Values"}
             {:name :default-value :label "Default Value"}
             {:name :labels :label "Labels" :type :i18ntext}])

(def type-name types/meta-item)
(def type-label "Meta Item")
(def type-path "meta-item")
(def type-kw (keyword type-name))

;; Register View Meta Items Process
(defprocess view-meta-items
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (let [nodes (services/get-meta-items org-id)
            protocol-version-id (lookup/get-protocol-version-in-query ctx)
            prot-props (if protocol-version-id {:protocol-version protocol-version-id} {})
            params (merge {:organization org-id} prot-props)]
        (if (meta nodes)
          (error/view-service-error nodes)
          (layout/render ctx (str type-label "s")
                         (container/scrollbox 
                           (list/selectlist {:action :.detail-action}
                                            (for [n nodes]
                                              {:label (:name n) :data (select-keys n [:id])})))
                         (actions/actions 
                           (if protocol-version-id
                             (actions/assign-action 
                               {:url (str "/api/" type-path "/assign") 
                                :params (merge params {type-kw :selected#id})
                                :verify (actions/gen-verify-a-selected "Meta Item")}))
                           (actions/details-action 
                             {:url (str "/view/" type-path) 
                              :params (merge params {type-kw :selected#id})
                              :verify (actions/gen-verify-a-selected "Meta Item")})
                           (actions/new-action 
                             {:url (str "/view/" type-path "/new") :params {:organization org-id}})
                           (actions/back-action)))))
      (ajax/forbidden))))
    
(as-method view-meta-items endpoint/endpoints "get-view-meta-items")
(as-method view-meta-items endpoint/endpoints "get-view-protocol-version-meta-item-add")

;; Register View Meta Item Process
(defprocess view-meta-item
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (if-let [node-id (lookup/get-meta-item-in-query ctx)]
        (let [n (services/get-meta-item node-id)
              editable (common/owned-by-user-org n)
              langs (services/get-languages org-id)]
          (if (meta n)
            (error/view-service-error n)
            (layout/render ctx (str type-label ": " (:name n))
                           (container/scrollbox 
                             (form/dataform 
                               (form/render-fields 
                                 {:editable editable
                                  :fields {:labels {:languages langs 
                                                    :default-language (get-in n [:organization :language])
                                                    :url "/api/text/i18n"
                                                    :params {:parent-id node-id
                                                             :parent-type type-name
                                                             :property :labels}}}} fields n)))
                           (actions/actions
                             (if editable
                               (list
                                 (actions/save-action 
                                   {:url (str "/api/" type-path) :params {type-kw node-id}})
                                 (actions/delete-action 
                                   {:url (str "/api/" type-path) :params {type-kw node-id}})))
                             (actions/back-action)))))
        ;; Handle Error
        (layout/render-error ctx {:message "An meta-item type is required."}))
      (ajax/forbidden))))

(as-method view-meta-item endpoint/endpoints "get-view-meta-item")

;; Register View New Meta Item Process
(defprocess view-meta-item-new
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (let [org (services/get-organization org-id)
            langs (services/get-languages org-id)]
        (layout/render ctx (str "Create " type-label)
                       (container/scrollbox 
                         (form/dataform 
                           (form/render-fields {:fields 
                                                {:labels {:languages langs
                                                          :default-language (:language org)}}} fields {})))
                       (actions/actions 
                         (actions/create-action 
                           {:url (str "/api/" type-path) :params {:organization org-id}})
                         (actions/back-action))))
      (ajax/forbidden))))

(as-method view-meta-item-new endpoint/endpoints "get-view-meta-item-new")

;; Register Assign Meta Item Process
(defprocess assign
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (let [meta-item-id (lookup/get-meta-item-in-query ctx)
            protocol-version-id (lookup/get-protocol-version-in-query ctx)
            resp (services/assign-meta-item-to-protocol-version meta-item-id protocol-version-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method assign endpoint/endpoints "post-api-meta-item-assign")

;; Register Create Meta Item Process
(defprocess create
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (let [body (assoc (:body-params ctx) :organization {:id org-id})
            resp (services/add-meta-item org-id body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method create endpoint/endpoints "put-api-meta-item")

;; Register Update Meta Item Process
(defprocess update
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (let [body (:body-params ctx)
            meta-item-id (lookup/get-meta-item-in-query ctx)
            resp (services/update-meta-item meta-item-id body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method update endpoint/endpoints "post-api-meta-item")

;; Register Update Meta Item Process
(defprocess delete
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (let [meta-item-id (lookup/get-meta-item-in-query ctx)
            resp (services/delete-meta-item meta-item-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method delete endpoint/endpoints "delete-api-meta-item")

