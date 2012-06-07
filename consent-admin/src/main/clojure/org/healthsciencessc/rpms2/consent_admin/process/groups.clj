(ns org.healthsciencessc.rpms2.consent-admin.process.groups
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.process-engine.path :as path]
            [org.healthsciencessc.rpms2.consent-admin.ui.layout :as layout]
            [org.healthsciencessc.rpms2.consent-admin.config :as config]
            [org.healthsciencessc.rpms2.consent-admin.ajax :as ajax]
            [org.healthsciencessc.rpms2.consent-admin.security :as security]
            [org.healthsciencessc.rpms2.consent-admin.ui.container :as container]
            [org.healthsciencessc.rpms2.consent-admin.ui.actions :as actions]
            [org.healthsciencessc.rpms2.consent-admin.ui.selectlist :as selectlist]
            [org.healthsciencessc.rpms2.consent-admin.ui.form :as formui]
            [sandbar.stateful-session :as sess]
            [org.healthsciencessc.rpms2.consent-admin.services :as service]
            [hiccup.core :as html]
            [hiccup.element :as elem]
            [hiccup.form :as form]
            [ring.util.response :as rutil])
  (:use [clojure.pprint]
        [org.healthsciencessc.rpms2.consent-admin.process.users :only (format-name)])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(defn layout-groups
  [ctx]
  (let [groups (service/get-groups ctx)]
    (if (service/service-error? groups)
      (ajax/error (meta groups))
      (layout/render ctx "Groups"
        (container/scrollbox
          (selectlist/selectlist {:action :.detail-action}
            (for [x (sort-by :name groups)]
              {:label (:name x) :data x})))
        (actions/actions 
             (actions/details-button {:url "/view/group/edit" :params {:group :selected#id}})
             (actions/new-button {:url "/view/group/add"})
             (actions/back-action))))))

(defn userlist [users]
  (container/scrollbox
    (selectlist/selectlist {:action :.detail-action}
      (for [u users]
        {:label (format-name u)
         :data u}))))

(def ^:const group-fields
  (let [text-fields [:name "Group name"
                     :code "Code"]]
    (map #(zipmap [:name :label] %)
         (partition 2 text-fields))))

(defn render-group-fields
  "Create some field boxes from a map of [kw text-label]"
  ([] (render-group-fields {}))
  ([group]
    (map formui/record->editable-field 
         (repeat group)
         group-fields))) 

(defn get-view-group-add
  [ctx]
  (layout/render ctx "Create Group"
                 (container/scrollbox (formui/dataform (render-group-fields)))
                 (actions/actions 
                   (actions/save-button {:method :post :url "/api/group/add"})
                   (actions/back-action))))

(defn get-view-group-edit
  [ctx]
  (if-let [group-id (-> ctx :query-params :group)]
    (let [group (service/get-group group-id)]
      (if (service/service-error? group)
        (ajax/error (meta group))
        (layout/render ctx "Edit Group"
                   (container/scrollbox (formui/dataform (render-group-fields group)))
                   (actions/actions 
                     (actions/save-button {:method :post :url "/api/group/edit" :params {:group group-id}})
                     (actions/details-button {:url "/view/group/members" :params {:group group-id} :label "Members"})
                     (actions/delete-button {:url "/api/group" :params {:group group-id}})
                     (actions/back-action)))))))

(defn get-view-group-members [ctx]
  (let [group-id (-> ctx :query-params :group)
        {in :in :as all} (service/get-group-members group-id)]
    (layout/render ctx "Members"
                   (userlist in)
                   (actions/actions
                     (actions/details-button {:label "Add User..."
                                              :url "/view/group/adduser"
                                              :params {:group group-id}})
                     (actions/delete-button {:label "Remove Selected User"
                                             :url "/api/group/member"
                                             :params {:user :selected#id
                                                      :group group-id}})
                     (actions/pop-button)))))

(defn get-view-group-adduser [ctx]
  (let [group-id (-> ctx :query-params :group)
           {out :out :as all} (service/get-group-members group-id)]
    (layout/render ctx "Members"
                   (userlist out)
                   (actions/actions
                     (actions/save-button {:label "Add Selected User"
                                           :method :put
                                           :url "/api/group/member"
                                           :params {:group group-id
                                                    :user :selected#id}})
                     (actions/pop-button)))))

(defn delete-api-group
  [ctx]
  (let [group (:group (:query-params ctx))
        resp (service/delete-group group)]
    (if (service/service-error? resp)
      (ajax/error (meta resp))
      (ajax/success resp))))

(defn post-api-group-add
  [ctx]
  (let [group (select-keys (:body-params ctx)
                          (map :name group-fields))
        resp (service/add-group group)]
    (if (service/service-error? resp)
      (ajax/error (meta resp))
      (ajax/success resp))))

(defn put-api-group-member
  [ctx]
  (let [{:keys [user group]} (:query-params ctx)
        resp (service/add-group-member group user)]
    (if (service/service-error? resp)
      (ajax/error (meta resp))
      (ajax/success resp))))

(defn delete-api-group-member
  [ctx]
  (let [{:keys [user group]} (:query-params ctx)
        resp (service/remove-group-member group user)]
    (if (service/service-error? resp)
      (ajax/error (meta resp))
      (ajax/success resp))))


(defn post-api-group-edit
  [ctx]
  (let [keys (select-keys (:body-params ctx)
                          (map :name group-fields))
        resp (service/edit-group (-> ctx :query-params :group)
                                keys)]
    (if (service/service-error? resp)
      (ajax/error (meta resp))
      (ajax/success resp))))

(def process-defns
  [{:name "get-view-groups"
    :runnable-fn (constantly true)
    :run-fn  layout-groups}
   {:name "get-view-group-add"
    :runnable-fn (constantly true)
    :run-fn get-view-group-add}
   {:name "delete-api-group"
    :runnable-fn (constantly true)
    :run-fn delete-api-group}
   {:name "get-view-group-edit"
    :runnable-fn (constantly true)
    :run-fn get-view-group-edit}
   {:name "get-view-group-adduser"
    :runnable-fn (constantly true)
    :run-fn get-view-group-adduser}
   {:name "get-view-group-members"
    :runnable-fn (constantly true)
    :run-fn get-view-group-members}
   {:name "post-api-group-edit"
    :runnable-fn (constantly true)
    :run-fn post-api-group-edit}
   {:name "delete-api-group-member"
    :runnable-fn (constantly true)
    :run-fn delete-api-group-member}
   {:name "put-api-group-member"
    :runnable-fn (constantly true)
    :run-fn put-api-group-member}
   {:name "post-api-group-add"
    :runnable-fn (constantly true)
    :run-fn post-api-group-add}
   ])

(process/register-processes (map #(DefaultProcess/create %) process-defns))
