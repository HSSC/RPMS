(ns org.healthsciencessc.rpms2.consent-admin.process.users
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.process-engine.path :as path]
            [org.healthsciencessc.rpms2.consent-admin.config :as config]
            [org.healthsciencessc.rpms2.consent-admin.ajax :as ajax]
            [org.healthsciencessc.rpms2.consent-admin.security :as security]
            [org.healthsciencessc.rpms2.consent-admin.ui.layout :as layout]
            [org.healthsciencessc.rpms2.consent-admin.ui.container :as container]
            [org.healthsciencessc.rpms2.consent-admin.ui.actions :as actions]
            [org.healthsciencessc.rpms2.consent-admin.ui.selectlist :as selectlist]
            [org.healthsciencessc.rpms2.consent-admin.ui.form :as formui]
            [org.healthsciencessc.rpms2.consent-admin.services :as service]
            [sandbar.stateful-session :as sess]
            [hiccup.core :as html]
            [hiccup.element :as elem]
            [hiccup.form :as form]
            [ring.util.response :as rutil])
  (:use [clojure.pprint])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(defn format-name
  [{:keys [first-name last-name middle-name]}]
  (str last-name ", " first-name " " middle-name))

(defn layout-users
  [ctx]
  (let [users (service/get-users ctx)]
    (if (service/service-error? users)
      (ajax/error (meta users))
      (layout/render ctx "Users"
        (container/scrollbox
          (selectlist/selectlist 
            (for [x users]
              {:label (format-name x) :data x})))
        (actions/actions 
             (actions/details-button {:url "/view/user/edit" :params {:user :selected#id}})
             (actions/details-button {:url "/view/role/addto" :params {:user :selected#id} :label "Add/Change roles"})
             (actions/new-button {:url "/view/user/add"})
             (actions/pop-button))))))

(def ^:const user-fields ;; probably should be i18nized
  (let [text-fields [:first-name "First Name"
                     :middle-name "Middle Name"
                     :last-name "Last Name"
                     :suffix "Suffix"
                     :title "Title"
                     :username "Username"]]
    (concat (map #(zipmap [:name :label] %)
                 (partition 2 text-fields))
            [{:type :password
              :name :password
              :label "Password"}])))

(defn render-user-fields
  "Create some field boxes from a map of [kw text-label]"
  ([] (render-user-fields {}))
  ([user]
    (map formui/record->editable-field 
         (repeat user)
         user-fields))) 

(defn get-view-user-add
  [ctx]
  (layout/render ctx "Create User"
                 (container/scrollbox (formui/dataform (render-user-fields)))
                 (actions/actions 
                   (actions/save-button {:method :post :url "/api/user/add"})
                   (actions/pop-button))))

(defn get-view-user-edit
  [ctx]
  (if-let [user-id (-> ctx :query-params :user)]
    (let [user (service/get-user user-id)]
      (if (service/service-error? user)
        (ajax/error (meta user))
        (layout/render ctx "Edit Organization"
                   (container/scrollbox (formui/dataform (render-user-fields user)))
                   (actions/actions 
                     (actions/save-button {:method :post :url "/api/user/edit" :params {:user user-id}})
                     (actions/pop-button)))))))

(defn post-api-user-add
  [ctx]
  (let [user (select-keys (:body-params ctx)
                          (map :name user-fields))
        resp (service/add-user user)]
    (if (service/service-error? resp)
      (ajax/error (meta resp))
      (ajax/success resp))))

(defn post-api-user-edit
  [ctx]
  (let [keys (select-keys (:body-params ctx)
                          (map :name user-fields))
        resp (service/edit-user (-> ctx :query-params :user)
                                keys)]
    (if (service/service-error? resp)
      (ajax/error (meta resp))
      (ajax/success resp))))

(def process-defns
  [{:name "get-view-users"
    :runnable-fn (constantly true)
    :run-fn  layout-users}
   {:name "get-view-user-add"
    :runnable-fn (constantly true)
    :run-fn get-view-user-add}
   {:name "get-view-user-edit"
    :runnable-fn (constantly true)
    :run-fn get-view-user-edit}
   {:name "post-api-user-edit"
    :runnable-fn (constantly true)
    :run-fn post-api-user-edit}
   {:name "post-api-user-add"
    :runnable-fn (constantly true)
    :run-fn post-api-user-add}
   ])

(process/register-processes (map #(DefaultProcess/create %) process-defns))
