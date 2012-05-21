(ns org.healthsciencessc.rpms2.consent-admin.process.roles
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
  (:use [clojure.pprint])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(defn layout-roles
  [ctx]
  (let [roles (service/get-roles ctx)]
    (if (service/service-error? roles)
      (ajax/error (meta roles))
      (layout/render ctx "Roles"
        (container/scrollbox
          (selectlist/selectlist 
            (for [x roles]
              {:label (:name x) :data x})))
        (actions/actions 
             (actions/details-button {:url "/view/role/edit" :params {:role :selected#id}})
             (actions/details-button {:url "/view/role/add" :params {:role :selected#id} :label "Change Roles"})
             (actions/new-button {:url "/view/role/add"})
             (actions/pop-button))))))

(def ^:const role-fields ;; probably should be i18nized
  (let [text-fields [:name "Role name"
                     :code "Code"]]
    (map #(zipmap [:name :label] %)
         (partition 2 text-fields))))

(defn render-role-fields
  "Create some field boxes from a map of [kw text-label]"
  ([] (render-role-fields {}))
  ([role]
    (map formui/record->editable-field 
         (repeat role)
         role-fields))) 

(defn get-view-role-add
  [ctx]
  (layout/render ctx "Create role"
                 (container/scrollbox (formui/dataform (render-role-fields)))
                 (actions/actions 
                   (actions/save-button {:method :post :url "/api/role/add"})
                   (actions/pop-button))))

(defn get-view-role-edit
  [ctx]
  (if-let [role-id (-> ctx :query-params :role)]
    (let [role (service/get-role role-id)]
      (if (service/service-error? role)
        (ajax/error (meta role))
        (layout/render ctx "Edit Organization"
                   (container/scrollbox (formui/dataform (render-role-fields role)))
                   (actions/actions 
                     (actions/save-button {:method :post :url "/api/role/edit" :params {:role role-id}})
                     (actions/pop-button)))))))

(defn post-api-role-add
  [ctx]
  (let [role (select-keys (:body-params ctx)
                          (map :name role-fields))
        resp (service/add-role role)]
    (if (service/service-error? resp)
      (ajax/error (meta resp))
      (ajax/success resp))))

(defn post-api-role-edit
  [ctx]
  (let [keys (select-keys (:body-params ctx)
                          (map :name role-fields))
        resp (service/edit-role (-> ctx :query-params :role)
                                keys)]
    (if (service/service-error? resp)
      (ajax/error (meta resp))
      (ajax/success resp))))

(def process-defns
  [{:name "get-view-roles"
    :runnable-fn (constantly true)
    :run-fn  layout-roles}
   {:name "get-view-role-add"
    :runnable-fn (constantly true)
    :run-fn get-view-role-add}
   {:name "get-view-role-edit"
    :runnable-fn (constantly true)
    :run-fn get-view-role-edit}
   {:name "post-api-role-edit"
    :runnable-fn (constantly true)
    :run-fn post-api-role-edit}
   {:name "post-api-role-add"
    :runnable-fn (constantly true)
    :run-fn post-api-role-add}
   ])

(process/register-processes (map #(DefaultProcess/create %) process-defns))
