;; Provides the configuration of the protocol managemant UIs.
(ns org.healthsciencessc.consent.manager.process.endorsement
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
            
            [pliant.webpoint.request :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))

(def fields [{:name :name :label "Name" :required true}
             {:name :code :label "Code"}
             {:name :endorsement-type :label "Type" :type :singleselect :required true :blank true :parser :id}
             {:name :labels :label "Labels" :type :i18ntext}])

(def type-name types/endorsement)
(def type-label "Endorsement")
(def type-path "endorsement")
(def type-kw (keyword type-name))

(defn- gen-endorsement-type-items
  [org-id]
  (let [types (services/get-endorsement-types org-id)
        items (map 
                (fn [t] {:label (:name t) 
                   :data (:id t) 
                   :item (select-keys t [:id])}) 
                types)]
    items))

;; Register View Endorsements Process
(defprocess view-endorsements
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (let [nodes (services/get-endorsements org-id)
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
                                :verify (actions/gen-verify-a-selected "Endorsement")}))
                           (actions/details-action 
                             {:url (str "/view/" type-path) 
                              :params (merge params {type-kw :selected#id})
                              :verify (actions/gen-verify-a-selected "Endorsement")})
                           (actions/new-action 
                             {:url (str "/view/" type-path "/new") :params params})
                           (actions/back-action)))))
      (ajax/forbidden))))

(as-method view-endorsements endpoint/endpoints "get-view-endorsements")
(as-method view-endorsements endpoint/endpoints "get-view-protocol-version-endorsement-add")

;; Register View Endorsement Process
(defprocess view-endorsement
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (if-let [node-id (lookup/get-endorsement-in-query ctx)]
        (let [n (services/get-endorsement node-id)
              endorsement-type (get-in n [:endorsement-type :id])
              langs (services/get-languages org-id)]
          (if (meta n)
            (error/view-service-error n)
            (layout/render ctx (str type-label ": " (:name n))
                           (container/scrollbox 
                             (form/dataform 
                               (form/render-fields 
                                 {:fields {:endorsement-type {:readonly true
                                                              :items (gen-endorsement-type-items org-id)}
                                           :labels {:languages langs 
                                                    :default-language (get-in n [:organization :language])
                                                    :url "/api/text/i18n"
                                                    :params {:parent-id node-id
                                                             :parent-type type-name
                                                             :property :labels}}}} fields n)))
                           (actions/actions
                             (actions/details-action 
                               {:url (str "/view/" type-path "/types") 
                                :params {:organization org-id :endorsement node-id :endorsement-type endorsement-type}
                                :label "Change Type"})
                             (actions/save-action 
                               {:url (str "/api/" type-path) :params {type-kw node-id}})
                             (actions/delete-action 
                               {:url (str "/api/" type-path) :params {type-kw node-id}})
                             (actions/back-action)))))
        ;; Handle Error
        (layout/render-error ctx {:message "An endorsement type is required."}))
      (ajax/forbidden))))

(as-method view-endorsement endpoint/endpoints "get-view-endorsement")

;; Register View New Endorsement Process
(defprocess view-endorsement-new
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (let [org (services/get-organization org-id)
            langs (services/get-languages org-id)]
        (layout/render ctx (str "Create " type-label)
                       (container/scrollbox 
                         (form/dataform 
                           (form/render-fields 
                             {:fields {:endorsement-type {:items (gen-endorsement-type-items org-id)}
                                       :labels {:languages langs 
                                                :default-language (:language org)}}} fields {})))
                       (actions/actions 
                         (actions/create-action 
                           {:url (str "/api/" type-path) :params {:organization org-id}})
                         (actions/back-action))))
      (ajax/forbidden))))

(as-method view-endorsement-new endpoint/endpoints "get-view-endorsement-new")

;; Register Assign Endorsement Process
(defprocess assign
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (let [endorsement-id (lookup/get-endorsement-in-query ctx)
            protocol-version-id (lookup/get-protocol-version-in-query ctx)
            resp (services/assign-endorsement-to-protocol-version endorsement-id protocol-version-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method assign endpoint/endpoints "post-api-endorsement-assign")

;; Register Create Endorsement Process
(defprocess create
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (let [body (assoc (:body-params ctx) :organization {:id org-id})
            resp (services/add-endorsement org-id body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method create endpoint/endpoints "put-api-endorsement")

;; Register Update Endorsement Process
(defprocess update
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (let [body (:body-params ctx)
            endorsement-id (lookup/get-endorsement-in-query ctx)
            resp (services/update-endorsement endorsement-id body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method update endpoint/endpoints "post-api-endorsement")

;; Register Update Endorsement Process
(defprocess delete
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (let [endorsement-id (lookup/get-endorsement-in-query ctx)
            resp (services/delete-endorsement endorsement-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method delete endpoint/endpoints "delete-api-endorsement")
