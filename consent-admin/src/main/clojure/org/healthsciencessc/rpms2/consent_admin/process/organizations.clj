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


(def fields [{:name :name :label "Name" :required true}
             {:name :code :label "Code"}
             {:name :protocol-label :label "Protocol Label"}
             {:name :location-label :label "Location Label"}
             {:name :consenter-label :label "Consenter Label"}
             {:name :language :label "Default Language" :type :singleselect :required true :blank true :parser :id}])

(defn- gen-language-items
  []
  (let [langs (service/get-languages)
        items (map 
                (fn [t] {:label (:name t) 
                   :data (:id t) 
                   :item (select-keys t [:id])}) 
                langs)]
    items))

(defn layout-organizations
  [ctx]
  (let [orgs (service/get-organizations ctx)]
    (if (service/service-error? orgs)
      (ajax/error (meta orgs))
      (layout/render ctx "Organizations"
        (container/scrollbox (selectlist/selectlist {:action :.detail-action}
          (for [x (->> orgs
                    (remove #(= code-base-org (:code %)))
                    (sort-by :name))]
            {:label (:name x) :data x})))
        (actions/actions 
             (actions/details-action {:url "/view/organization/edit" :params {:organization :selected#id}
                            :verify (actions/gen-verify-a-selected "Organization")})
             (actions/new-action {:label "New" :url "/view/organization/add"})
             (actions/back-action))))))

(defn get-view-organization-add
  [ctx]
  (layout/render ctx "Create Organization"
                   (container/scrollbox 
                     (formui/dataform 
                       (formui/render-fields 
                         {:fields {:language {:items (gen-language-items)}}} fields {})))
                 (actions/actions 
                   (actions/save-action {:method :post :url "/api/organization/add"})
                   (actions/back-action))))

(defn delete-api-organization
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
                   (container/scrollbox 
                     (formui/dataform 
                       (formui/render-fields 
                         {:fields {:language {:items (gen-language-items)}}} fields org)))
                   (actions/actions 
                     (actions/details-action {:url "/view/user/add" :params {:organization org-id} :label "Add Administrator"})
                     (actions/delete-action {:label "Delete" :url "/api/organization" :params {:organization org-id} :confirm "Are you sure you want to delete this organization?"})
                     (actions/save-action {:method :post :url "/api/organization/edit" :params {:organization org-id}})
                     (actions/back-action)))))))

(defn post-api-organization-add
  [ctx]
  (let [org (select-keys (:body-params ctx)
                         [:name :code :protocol-label :location-label :consenter-label])
        resp (service/add-organization org)]
    (if (service/service-error? resp)
      (ajax/error (meta resp))
      (ajax/success resp))))

(defn post-api-organization-edit
  [ctx]
  (let [keys (select-keys (:body-params ctx)
                              [:name :code :protocol-label :location-label :consenter-label])
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
   {:name "delete-api-organization"
    :runnable-fn (constantly true)
    :run-fn delete-api-organization}
   {:name "post-api-organization-edit"
    :runnable-fn (constantly true)
    :run-fn post-api-organization-edit}
   {:name "post-api-organization-add"
    :runnable-fn (constantly true)
    :run-fn post-api-organization-add}
   ])

(process/register-processes (map #(DefaultProcess/create %) process-defns))
