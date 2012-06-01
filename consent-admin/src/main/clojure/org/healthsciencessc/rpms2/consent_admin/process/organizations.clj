(ns org.healthsciencessc.rpms2.consent-admin.process.organizations
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
        [org.healthsciencessc.rpms2.consent-domain.types :only (code-base-org)])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(defn layout-organizations
  [ctx]
  (let [orgs (service/get-organizations ctx)]
    (if (service/service-error? orgs)
      (ajax/error (meta orgs))
      (layout/render ctx "Organizations"
        (container/scrollbox (selectlist/selectlist 
          (for [x (->> orgs
                    (remove #(= code-base-org (:code %)))
                    (sort-by :name))]
            {:label (:name x) :data x})))
        (actions/actions 
             (actions/details-button {:url "/view/organization/edit" :params {:organization :selected#id}})
             (actions/details-button {:label "Delete" :url "/view/organization/delete" :params {:organization :selected#id}})
             (actions/new-button {:label "New" :url "/view/organization/add"})
             (actions/pop-button))))))

(defn create-fields [{:keys [name code protocol-label location-label]}]
  (list
      (formui/input-text {:name :name :label "Name" :value name})
      (formui/input-text {:name :code :label "Code" :value code})
      (formui/input-text {:name :protocol-label :label "Protocol Label" :value protocol-label})
      (formui/input-text {:name :location-label :label "Location Label" :value location-label})))

(defn get-view-organization-add
  [ctx]
  (layout/render ctx "Create Organization"
                 (container/scrollbox (formui/dataform (create-fields {})))
                 (actions/actions 
                   (actions/save-button {:method :post :url "/api/organization/add"})
                   (actions/pop-button))))

(defn get-view-organization-delete
  [ctx]
  (let [org-id (:organization (:query-params ctx))]
    (layout/render ctx "Delete Organization"
                   (container/scrollbox 
                     [:h1.confirm "This is dangerous, please confirm deleting."])
                   (actions/actions 
                     (actions/save-button {:label "Delete Organization"
                                           :method :post
                                           :params {:organization org-id}
                                           :url "/api/organization/delete"})
                     (actions/pop-button)))))

(defn post-api-organization-delete
  [ctx]
  (let [org-id (:organization (:query-params ctx))
        resp (service/delete-organization org-id)]
    (if (service/service-error? resp)
      (ajax/error (meta resp))
      (ajax/success resp))))

(defn get-view-organization-edit
  [ctx]
  (if-let [org-id (-> ctx :query-params :organization)]
    (let [org (service/get-organization org-id)]
      (if (service/service-error? org)
        (ajax/error (meta org))
        (layout/render ctx "Edit Organization"
                   (container/scrollbox (formui/dataform (create-fields org)))
                   (actions/actions 
                     (actions/details-button {:url "/view/user/add" :params {:organization org-id} :label "Add Administrator"})
                     (actions/save-button {:method :post :url "/api/organization/edit" :params {:organization org-id}})
                     (actions/pop-button)))))))

(defn post-api-organization-add
  [ctx]
  (let [org (select-keys (:body-params ctx)
                         [:name :code :protocol-label :location-label])
        resp (service/add-organization org)]
    (if (service/service-error? resp)
      (ajax/error (meta resp))
      (ajax/success resp))))

(defn post-api-organization-edit
  [ctx]
  (let [keys (select-keys (:body-params ctx)
                              [:name :code :protocol-label :location-label])
        resp (service/edit-organization (-> ctx :query-params :organization) keys)]
    (if (service/service-error? resp)
      (ajax/error (meta resp))
      (ajax/success resp))))

(def process-defns
  [{:name "get-view-organizations"
    :runnable-fn (constantly true)
    :run-fn  layout-organizations}
   {:name "get-view-organization"
    :runnable-fn (constantly true)
    :run-fn #(process/dispatch "get-view-organization-edit"  ;; I'm so ashamed
                               (assoc-in %
                                         [:query-params :organization]
                                         (get-in (sess/session-get :user)
                                                 [:organization :id])))}
   {:name "get-view-organization-add"
    :runnable-fn (constantly true)
    :run-fn get-view-organization-add}
   {:name "get-view-organization-edit"
    :runnable-fn (constantly true)
    :run-fn get-view-organization-edit}
   {:name "get-view-organization-delete"
    :runnable-fn (constantly true)
    :run-fn get-view-organization-delete}
   {:name "post-api-organization-edit"
    :runnable-fn (constantly true)
    :run-fn post-api-organization-edit}
   {:name "post-api-organization-delete"
    :runnable-fn (constantly true)
    :run-fn post-api-organization-delete}
   {:name "post-api-organization-add"
    :runnable-fn (constantly true)
    :run-fn post-api-organization-add}
   ])

(process/register-processes (map #(DefaultProcess/create %) process-defns))
