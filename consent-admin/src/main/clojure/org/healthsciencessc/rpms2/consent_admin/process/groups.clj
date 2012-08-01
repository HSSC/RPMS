(ns org.healthsciencessc.rpms2.consent-admin.process.groups
  (:require [org.healthsciencessc.rpms2.consent-admin.ajax :as ajax]
            [org.healthsciencessc.rpms2.consent-client.core :as services]
            [org.healthsciencessc.rpms2.consent-admin.security :as security]
            
            [org.healthsciencessc.rpms2.consent-admin.process.common :as common]
            [org.healthsciencessc.rpms2.consent-admin.process.users :as user]
            
            [org.healthsciencessc.rpms2.consent-admin.ui.actions :as actions]
            [org.healthsciencessc.rpms2.consent-admin.ui.container :as container]
            [org.healthsciencessc.rpms2.consent-admin.ui.form :as form]
            [org.healthsciencessc.rpms2.consent-admin.ui.layout :as layout]
            [org.healthsciencessc.rpms2.consent-admin.ui.list :as list]
            
            [org.healthsciencessc.rpms2.consent-domain.lookup :as lookup]
            [org.healthsciencessc.rpms2.consent-domain.roles :as roles]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
            
            [ring.util.response :as rutil]
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))

(def fields [{:name :name :label "Name"}
             {:name :code :label "Code"}])

(def type-name types/group)
(def type-label "Group")
(def type-path "group")
(def type-kw (keyword type-name))

(defn userlist [users]
  (container/scrollbox
    (list/selectlist {:action :.detail-action}
      (for [u users]
        {:label (user/format-name u)
         :data u}))))

;; Register View Groups Process
(defprocess view-groups
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-admin-org-id? user org-id)
      (let [nodes (services/get-groups org-id)]
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
    
(as-method view-groups endpoint/endpoints "get-view-groups")

;; Register View Group Process
(defprocess view-group
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-admin-org-id? user org-id)
      (if-let [node-id (lookup/get-group-in-query ctx)]
        (let [n (services/get-group node-id)
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
                             (actions/details-action {:url "/view/group/members" 
                                                      :params {:group node-id} 
                                                      :label "Members"})
                             (actions/details-action {:url "/view/roles/show" 
                                                      :params {:assignee-type :group :assignee-id node-id} 
                                                      :label "Roles"})
                             (actions/back-action)))))
        ;; Handle Error
        (layout/render-error ctx {:message "An group is required."}))
      (ajax/forbidden))))

(as-method view-group endpoint/endpoints "get-view-group")

;; Register View New Group Process
(defprocess view-group-new
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

(as-method view-group-new endpoint/endpoints "get-view-group-new")

;; Register Create Group Process
(defprocess create
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-admin-org-id? user org-id)
      (let [body (assoc (:body-params ctx) :organization {:id org-id})
            resp (services/add-group org-id body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method create endpoint/endpoints "put-api-group")

;; Register Update Group Process
(defprocess update
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-admin-org-id? user org-id)
      (let [body (:body-params ctx)
            group-id (lookup/get-group-in-query ctx)
            resp (services/update-group group-id body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method update endpoint/endpoints "post-api-group")

;; Register Delete Group Process
(defprocess delete
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-admin-org-id? user org-id)
      (let [group-id (lookup/get-group-in-query ctx)
            resp (services/delete-group group-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method delete endpoint/endpoints "delete-api-group")

;; Register View Group Members Process
(defprocess view-group-members
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-admin-org-id? user org-id)
      (if-let [group-id (lookup/get-group-in-query ctx)]
        (let [users (services/get-group-members group-id)]
          (layout/render ctx "Members"
                         (userlist (sort-by #(vec (map % [:last-name :first-name])) users))
                         (actions/actions
                           (actions/details-action {:label "Add Member..."
                                                    :url "/view/group/adduser"
                                                    :params {:group group-id}})
                           (actions/delete-action {:label "Remove Member"
                                                   :url "/api/group/member"
                                                   :params {:user :selected#id
                                                            :group group-id}
                                                   :action-on-success "refresh"})
                           (actions/back-action))))
        (layout/render-error ctx {:message "An group is required."}))
      (ajax/forbidden))))

(as-method view-group-members endpoint/endpoints "get-view-group-members")

;; Register View Add Group Members Process
(defprocess view-group-add-member
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-admin-org-id? user org-id)
      (if-let [group-id (lookup/get-group-in-query ctx)]
        (let [users (services/get-group-nonmembers group-id)]
          (layout/render ctx "Add Member"
                         (userlist (sort-by :last-name users))
                         (actions/actions
                           (actions/save-action {:label "Add Member"
                                                 :method :put
                                                 :url "/api/group/member"
                                                 :params {:group group-id
                                                          :user :selected#id}})
                           (actions/back-action))))
        (layout/render-error ctx {:message "An group is required."}))
      (ajax/forbidden))))

(as-method view-group-add-member endpoint/endpoints "get-view-group-adduser")

;; Register Add Group Member Process
(defprocess add-member
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-admin-org-id? user org-id)
      (let [group-id (lookup/get-group-in-query ctx)
            user-id (lookup/get-user-in-query ctx)
            resp (services/add-group-member group-id user-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method add-member endpoint/endpoints "put-api-group-member")


;; Register Remove Group Member Process
(defprocess remove-member
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-admin-org-id? user org-id)
      (let [group-id (lookup/get-group-in-query ctx)
            user-id (lookup/get-user-in-query ctx)
            resp (services/remove-group-member group-id user-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method remove-member endpoint/endpoints "delete-api-group-member")