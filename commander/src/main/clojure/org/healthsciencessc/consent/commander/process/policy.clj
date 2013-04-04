;; Provides the configuration of policy management.
(ns org.healthsciencessc.consent.commander.process.policy
  (:require [org.healthsciencessc.consent.commander.ajax :as ajax]
            [org.healthsciencessc.consent.commander.error :as error]
            [org.healthsciencessc.consent.commander.security :as security]
            [org.healthsciencessc.consent.client.core :as services]
            [org.healthsciencessc.consent.commander.process.common :as common]
            
            [org.healthsciencessc.consent.commander.ui.actions :as actions]
            [org.healthsciencessc.consent.commander.ui.container :as container]
            [org.healthsciencessc.consent.commander.ui.form :as form]
            [org.healthsciencessc.consent.commander.ui.layout :as layout]
            [org.healthsciencessc.consent.commander.ui.list :as list]
            
            [org.healthsciencessc.consent.domain.lookup :as lookup]
            [org.healthsciencessc.consent.domain.roles :as roles]
            [org.healthsciencessc.consent.domain.types :as types]
            
            [pliant.webpoint.request :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))

(def fields [{:name :name :label "Name" :required true}
             {:name :code :label "Code"}
             {:name :policy-definition :label "Policy Definition" :type :singleselect :required true :blank true :parser :id}
             {:name :titles :label "Titles" :type :i18ntext}
             {:name :texts :label "Texts" :type :i18ntext :paragraphs true}])

(def type-name types/policy)
(def type-label "Policy")
(def type-path "policy")
(def type-kw (keyword type-name))

(defn- gen-policy-definition-items
  [org-id]
  (let [types (services/get-policy-definitions org-id)
        items (map 
                (fn [t] {:label (:name t) 
                   :data (:id t) 
                   :item (select-keys t [:id])}) 
                types)]
    items))


;; Register View Languages Process
(defprocess view-policies
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (let [nodes (services/get-policies org-id)
            protocol-version-id (lookup/get-protocol-version-in-query ctx)
            prot-props (if protocol-version-id {:protocol-version protocol-version-id} {})
            params (merge {:organization org-id} prot-props)]
        (if (meta nodes)
          (error/view-service-error nodes)
          (layout/render ctx "Policies"
                         (container/scrollbox 
                           (list/selectlist {:action :.detail-action}
                                            (for [n nodes]
                                              {:label (:name n) :data (select-keys n [:id])})))
                         (actions/actions 
                           (if protocol-version-id
                             (actions/assign-action 
                               {:url (str "/api/" type-path "/assign") 
                                :params (merge params {type-kw :selected#id})
                                :verify (actions/gen-verify-a-selected "Policy")}))
                           (actions/details-action 
                             {:url (str "/view/" type-path)  
                              :params (merge params {type-kw :selected#id})
                              :verify (actions/gen-verify-a-selected "Policy")})
                           (actions/new-action 
                             {:url (str "/view/" type-path "/new") :params params})
                           (actions/back-action)))))
      (ajax/forbidden))))
    
(as-method view-policies endpoint/endpoints "get-view-policys")
(as-method view-policies endpoint/endpoints "get-view-protocol-version-policy-add")
    
;; Register View Policy Process
(defprocess view-policy
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (if-let [node-id (lookup/get-policy-in-query ctx)]
        (let [n (services/get-policy node-id)
              org-id (get-in n [:organization :id])
              policy-definition (get-in n [:policy-definition :id])
              langs (services/get-languages org-id)]
          (if (meta n)
            (error/view-service-error n)
            (layout/render ctx (str type-label ": " (:name n))
                           (container/scrollbox 
                             (form/dataform 
                               (form/render-fields 
                                 {:fields {:policy-definition {:readonly true
                                                               :items (gen-policy-definition-items org-id)}
                                           :titles {:languages langs 
                                                    :default-language (get-in n [:organization :language])
                                                    :url "/api/text/i18n"
                                                    :params {:parent-id node-id
                                                             :parent-type type-name
                                                             :property :titles}}
                                           :texts {:languages langs 
                                                   :default-language (get-in n [:organization :language])
                                                   :url "/api/text/i18n"
                                                   :params {:parent-id node-id
                                                            :parent-type type-name
                                                            :property :texts}}}} fields n)))
                           (actions/actions
                             ;;(actions/details-action 
                             ;;  {:url (str "/view/" type-path "/definitions") 
                             ;;   :params {:organization org-id :policy node-id :policy-definition policy-definition}
                             ;;   :label "Change Type"})
                             (actions/save-action 
                               {:url (str "/api/" type-path) :params {type-kw node-id}})
                             (actions/delete-action 
                               {:url (str "/api/" type-path) :params {type-kw node-id}})
                             (actions/back-action)))))
        ;; Handle Error
        (layout/render-error ctx {:message "An policy type is required."}))
      (ajax/forbidden))))

(as-method view-policy endpoint/endpoints "get-view-policy")


;; Register View New Policy Process
(defprocess view-policy-new
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (let [org-id (common/lookup-organization ctx)
            org (services/get-organization org-id)
            langs (services/get-languages org-id)]
        (layout/render ctx (str "Create " type-label)
                       (container/scrollbox 
                         (form/dataform 
                           (form/render-fields 
                             {:fields {:policy-definition {:items (gen-policy-definition-items org-id)}
                                       :titles {:languages langs
                                                :default-language (:language org)}
                                       :texts {:languages langs
                                               :default-language (:language org)}}} fields {})))
                       (actions/actions 
                         (actions/create-action 
                           {:url (str "/api/" type-path) :params {:organization org-id}})
                         (actions/back-action))))
      (ajax/forbidden))))

(as-method view-policy-new endpoint/endpoints "get-view-policy-new")

;; Register Assign Policy Process
(defprocess assign
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (let [policy-id (lookup/get-policy-in-query ctx)
            protocol-version-id (lookup/get-protocol-version-in-query ctx)
            resp (services/assign-policy-to-protocol-version policy-id protocol-version-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method assign endpoint/endpoints "post-api-policy-assign")

;; Register Create Policy Process
(defprocess create
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (let [body (assoc (:body-params ctx) :organization {:id org-id})
            resp (services/add-policy org-id body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method create endpoint/endpoints "put-api-policy")

;; Register Update Policy Process
(defprocess update
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (let [body (:body-params ctx)
            policy-id (lookup/get-policy-in-query ctx)
            resp (services/update-policy policy-id body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method update endpoint/endpoints "post-api-policy")

;; Register Update Policy Process
(defprocess delete
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (let [policy-id (lookup/get-policy-in-query ctx)
            resp (services/delete-policy policy-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method delete endpoint/endpoints "delete-api-policy")

