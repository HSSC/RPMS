(ns org.healthsciencessc.consent.commander.process.users
  (:require [org.healthsciencessc.consent.commander.ajax :as ajax]
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
            
            [ring.util.response :as rutil]
            [pliant.webpoint.request :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))

(def fields [{:name :first-name :label "First Name" :required true}
             {:name :middle-name :label "Middle"}
             {:name :last-name :label "Last Name" :required true}
             {:name :suffix :label "Suffix"}
             {:name :title :label "Title"}
             {:name :username :label "Username" :required true}
             {:name :password :label "Password" :type :password :required true}])
 
(def type-name types/user)
(def type-label "User")
(def type-path "user")
(def type-kw (keyword type-name))

(defn format-name
  [{:keys [first-name last-name middle-name]}]
  (str last-name ", " first-name " " middle-name))

;; Register View Users Process
(defprocess view-users
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-admin-org-id? user org-id)
      (let [nodes (services/get-users org-id)]
        (if (meta nodes)
          (rutil/not-found (:message (meta nodes)))
          (layout/render ctx (str type-label "s")
                         (container/scrollbox 
                           (list/selectlist {:action :.detail-action}
                                            (for [n (sort-by :last-name nodes)]
                                              {:label (format-name n) :data (select-keys n [:id])})))
                         (actions/actions 
                          (actions/details-action 
                               {:url (str "/view/" type-path) :params {:organization org-id type-kw :selected#id}
                                :verify (actions/gen-verify-a-selected type-label)})
                           (actions/new-action 
                             {:url (str "/view/" type-path "/new") :params {:organization org-id}})
                           (actions/back-action)))))
      (ajax/forbidden))))
    
(as-method view-users endpoint/endpoints "get-view-users")

;; Register View New User Process
(defprocess view-user-new
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-admin-org-id? user org-id)
      (layout/render ctx "Create User"
                     (container/scrollbox 
                       (form/dataform 
                         (form/render-fields {} fields)))
                     (actions/actions 
                       (actions/create-action 
                         {:url "/api/user" :params {:organization org-id}})
                       (actions/back-action)))
      (ajax/forbidden))))

(as-method view-user-new endpoint/endpoints "get-view-user-new")


;; Register View New User Process
(defprocess view-user-new-admin
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-admin-org-id? user org-id)
      (layout/render ctx "Create Admin"
                     (container/scrollbox 
                       (form/dataform 
                         (form/render-fields {} fields)))
                     (actions/actions 
                       (actions/create-action 
                         {:url "/api/user/admin" :params {:organization org-id}})
                       (actions/back-action)))
      (ajax/forbidden))))

(as-method view-user-new-admin endpoint/endpoints "get-view-user-new-admin")

;; Register View User Process
(defprocess view-user
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-admin-org-id? user org-id)
      (if-let [node-id (lookup/get-user-in-query ctx)]
        (let [n (services/get-user node-id)
              editable (common/owned-by-user-org n)]
          (if (meta n)
            (rutil/not-found (:message (meta n)))
            (layout/render ctx (str type-label ": " (format-name n))
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
                             (actions/details-action {:url "/view/roles/show" 
                                                      :params {:assignee-type :user :assignee-id node-id} 
                                                      :label "Roles"})
                             (actions/back-action)))))
        ;; Handle Error
        (layout/render-error ctx {:message "An user is required."}))
      (ajax/forbidden))))

(as-method view-user endpoint/endpoints "get-view-user")

;; Register Create User Process
(defprocess create
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-admin-org-id? user org-id)
      (let [body (:body-params ctx)
            id (select-keys body [:username :password :realm])
            clean-body (dissoc (assoc (:body-params ctx) :organization {:id org-id} :identity id)
                               :username :password :realm)
            resp (services/add-user org-id clean-body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method create endpoint/endpoints "put-api-user")


;; Register Create User Admin Process
(defprocess create-admin
  [ctx]
  (let [user (security/current-user ctx)
        org-id (lookup/get-organization-in-query ctx)]
    (if (roles/can-admin-org-id? user org-id)
      (let [body (:body-params ctx)
            id (select-keys body [:username :password :realm])
            clean-body (dissoc (assoc (:body-params ctx) :organization {:id org-id} :identity id)
                               :username :password :realm)
            resp (services/add-admin org-id clean-body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method create-admin endpoint/endpoints "put-api-user-admin")

;; Register Update User Process
(defprocess update
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-admin-org-id? user org-id)
      (let [body (:body-params ctx)
            user-id (lookup/get-user-in-query ctx)
            resp (services/update-user user-id body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method update endpoint/endpoints "post-api-user")

;; Register Delete User Process
(defprocess delete
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (roles/can-admin-org-id? user org-id)
      (let [user-id (lookup/get-user-in-query ctx)
            resp (services/delete-user user-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method delete endpoint/endpoints "delete-api-user")
