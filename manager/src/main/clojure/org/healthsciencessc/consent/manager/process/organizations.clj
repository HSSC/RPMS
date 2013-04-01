(ns org.healthsciencessc.consent.manager.process.organizations
  (:require [org.healthsciencessc.consent.manager.ajax :as ajax]
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
            
            [ring.util.response :as rutil]
            [pliant.webpoint.request :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))

(def fields [{:name :name :label "Name" :required true}
             {:name :code :label "Code"}
             {:name :protocol-label :label "Protocol Label"}
             {:name :location-label :label "Location Label"}
             {:name :consenter-label :label "Consenter Label"}
             {:name :encounter-label :label "Encounter Label"}
             {:name :language :label "Default Language" :type :singleselect :required true :blank true :parser :id}])

(def type-name types/organization)
(def type-label "Organization")
(def type-path "organization")
(def type-kw (keyword type-name))

(defn- gen-language-items
  [org-id]
  (let [langs (services/get-languages org-id)
        items (map 
                (fn [t] {:label (:name t) 
                   :data (:id t) 
                   :item (select-keys t [:id])}) 
                langs)]
    items))

;; Register View Organizations Process
(defprocess view-organizations
  [ctx]
  (let [user (security/current-user ctx)]
    (if (roles/superadmin? user)
      (let [org-id (lookup/get-organization-in-query ctx)
            orgs (services/get-organizations org-id)
            nodes (remove #(= types/code-base-org (:code %)) orgs)]
        (if (meta nodes)
          (rutil/not-found (:message (meta nodes)))
          (layout/render ctx (str type-label "s")
                         (container/scrollbox 
                           (list/selectlist {:action :.detail-action}
                                            (for [n (sort-by :name nodes)]
                                              {:label (:name n) :data (select-keys n [:id])})))
                         (actions/actions 
                           (actions/details-action 
                             {:url (str "/view/" type-path) :params {type-kw :selected#id}
                              :verify (actions/gen-verify-a-selected type-label)})
                           (if (roles/superadmin? user)
                             (actions/new-action 
                               {:url (str "/view/" type-path "/new") :params {}}))
                           (actions/back-action)))))
      (ajax/forbidden))))
    
(as-method view-organizations endpoint/endpoints "get-view-organizations")

;; Register View Organization Process
(defprocess view-organization
  [ctx]
  (let [user (security/current-user ctx)
        org-id (or (lookup/get-organization-in-query ctx) (security/current-org-id))]
    (if (roles/can-admin-org-id? user org-id)
      (let [org (services/get-organization org-id)]
        (if (meta org)
          (rutil/not-found (:message (meta org)))
          (layout/render ctx (str type-label ": " (:name org))
                         (container/scrollbox 
                           (form/dataform 
                             (form/render-fields 
                               {:fields {:language {:readonly true 
                                                    :items (gen-language-items org-id)}}} fields org)))
                         (actions/actions
                           (actions/save-action 
                             {:url (str "/api/" type-path) :params {type-kw org-id}})
                           (actions/details-action {:url "/view/languages" 
                                                        :params {:organization org-id} :label "Change Language"})
                           (if (roles/superadmin? user)
                             (list 
                               (actions/details-action {:url "/view/user/new/admin" 
                                                        :params {:organization org-id} :label "Add Administrator"})
                               (actions/delete-action 
                                 {:url (str "/api/" type-path) :params {type-kw org-id}})))
                           (actions/back-action)))))
      (ajax/forbidden))))

(as-method view-organization endpoint/endpoints "get-view-organization")

;; Register View New Organization Process
(defprocess view-organization-new
  [ctx]
  (let [user (security/current-user ctx)]
    (if (roles/superadmin? user)
      (layout/render ctx (str "Create " type-label)
                     (container/scrollbox 
                       (form/dataform 
                         (form/render-fields 
                           {:fields {:language {:items (gen-language-items (security/current-org-id))}}} fields {})))
                     (actions/actions 
                       (actions/create-action 
                         {:url (str "/api/" type-path) :params {}})
                       (actions/back-action)))
      (ajax/forbidden))))

(as-method view-organization-new endpoint/endpoints "get-view-organization-new")

;; Register Create Organization Process
(defprocess create
  [ctx]
  (let [user (security/current-user ctx)]
    (if (roles/superadmin? user)
      (let [body (:body-params ctx)
            resp (services/add-organization body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method create endpoint/endpoints "put-api-organization")

;; Register Update Organization Process
(defprocess update
  [ctx]
  (let [user (security/current-user ctx)
        org-id (lookup/get-organization-in-query ctx)]
    (if (roles/can-admin-org-id? user org-id)
      (let [body (:body-params ctx)
            resp (services/update-organization org-id body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method update endpoint/endpoints "post-api-organization")

;; Register Update Organization Process
(defprocess delete
  [ctx]
  (let [user (security/current-user ctx)]
    (if (roles/superadmin? user)
      (let [org-id (lookup/get-organization-in-query ctx)
            resp (services/delete-organization org-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method delete endpoint/endpoints "delete-api-organization")

