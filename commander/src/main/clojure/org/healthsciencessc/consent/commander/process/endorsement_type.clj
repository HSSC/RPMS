;; Provides the configuration of the protocol managemant UIs.
(ns org.healthsciencessc.consent.commander.process.endorsement-type
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
            
            [org.healthsciencessc.consent.common.lookup :as lookup]
            [org.healthsciencessc.consent.common.roles :as roles]
            [org.healthsciencessc.consent.common.types :as types]
            
            [pliant.webpoint.request :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))

(def fields [{:name :name :label "Name"}
             {:name :code :label "Code"}
             {:name :uri :label "URI"}])

(def type-name types/endorsement-type)
(def type-label "Endorsement Type")
(def type-path "endorsement/type")
(def type-kw (keyword type-name))

;; Register View Endorsement Types Process
(defprocess view-endorsement-types
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
        (let [nodes (services/get-endorsement-types org-id)
              endorsement-id (lookup/get-endorsement-in-query ctx)
              endorsement-type-id (lookup/get-endorsement-type-in-query ctx)]
          (if (meta nodes)
            (error/view-service-error nodes)
            (layout/render ctx (str type-label "s")
                           (container/scrollbox 
                             (list/selectlist {:action :.detail-action}
                                              (for [n nodes]
                                                {:label (:name n) :data n})))
                           (actions/actions 
                             (if endorsement-id
                               (actions/ajax-action 
                                 {:method :post :url (str "/api/" type-path "/assign") :label "Assign Type"
                                  :params {:organization org-id :assign-type :selected#id 
                                           :endorsement endorsement-id type-kw endorsement-type-id}
                                  :action-on-success ".back-action"
                                  :verify {:action "selected" :message "Please select an Endorsement Type."}})
                               (list
                                 (actions/details-action 
                                   {:url (str "/view/" type-path) :params {:organization org-id type-kw :selected#id}
                                    :verify (actions/gen-verify-a-selected "Endorsement Type")})
                                 (actions/new-action 
                                   {:url (str "/view/" type-path "/new") :params {:organization org-id}})))
                             (actions/back-action)))))
      (ajax/forbidden))))

(as-method view-endorsement-types endpoint/endpoints "get-view-endorsement-types")

;; Register View Endorsement Type Process
(defprocess view-endorsement-type
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (if-let [node-id (lookup/get-endorsement-type-in-query ctx)]
        (let [n (services/get-endorsement-type node-id)
              editable (= (get-in n [:organization :id]) (security/current-org-id))]
          (if (meta n)
            (error/view-service-error n)
            (layout/render ctx (str type-label ": " (:name n))
                           (container/scrollbox 
                             (form/dataform (form/render-fields {:editable editable} fields n)))
                           (actions/actions
                             (if editable
                               (list 
                                 (actions/save-action 
                                   {:url (str "/api/" type-path) :params {type-kw node-id}})
                                 (actions/delete-action 
                                   {:url (str "/api/" type-path) :params {type-kw node-id}})))
                             (actions/back-action)))))
        ;; Handle Error
        (layout/render-error ctx {:message "An endorsement type is required."}))
      (ajax/forbidden))))

(as-method view-endorsement-type endpoint/endpoints "get-view-endorsement-type")

;; Register View New Endorsement Type Process
(defprocess view-endorsement-type-new
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (layout/render ctx (str "Create " type-label)
                     (container/scrollbox (form/dataform (form/render-fields {} fields )))
                     (actions/actions 
                       (actions/create-action 
                         {:url (str "/api/" type-path) :params {:organization org-id}})
                       (actions/back-action)))
      (ajax/forbidden))))

(as-method view-endorsement-type-new endpoint/endpoints "get-view-endorsement-type-new")

;; Register Assign Endorsement Type Process
(defprocess assign
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (let [endorsement-id (lookup/get-endorsement-in-query ctx)
            new-type-id (get-in ctx [:query-params :assign-type])
            endorsement-type-id (lookup/get-endorsement-type-in-query ctx)
            resp (services/assign-endorsement-type endorsement-id endorsement-type-id new-type-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method assign endpoint/endpoints "post-api-endorsement-type-assign")

;; Register Create Endorsement Type Process
(defprocess create
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (let [body (assoc (:body-params ctx) :organization {:id org-id})
            resp (services/add-endorsement-type org-id body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method create endpoint/endpoints "put-api-endorsement-type")

;; Register Update Endorsement Type Process
(defprocess update
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (let [body (:body-params ctx)
            endorsement-type-id (lookup/get-endorsement-type-in-query ctx)
            resp (services/update-endorsement-type endorsement-type-id body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method update endpoint/endpoints "post-api-endorsement-type")

;; Register Update Endorsement Type Process
(defprocess delete
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-design-org-id? user org-id)
      (let [endorsement-type-id (lookup/get-endorsement-type-in-query ctx)
            resp (services/delete-endorsement-type endorsement-type-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method delete endpoint/endpoints "delete-api-endorsement-type")
