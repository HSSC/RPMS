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
  (:use [clojure.pprint]
        [org.healthsciencessc.rpms2.consent-domain.tenancy :only (label-for-location)]
        [clojure.java.io :as io])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(defn layout-roles
  [ctx]
  (let [roles (service/get-roles)]
    (if (service/service-error? roles)
      (ajax/error (meta roles))
      (layout/render ctx "Roles"
        (container/scrollbox
          (selectlist/selectlist {:action :.detail-action}
            (for [x (sort-by :name roles)]
              {:label (:name x) :data x})))
        (actions/actions 
             (actions/details-action {:url "/view/role/edit" :params {:role :selected#id}})
             (actions/new-action {:url "/view/role/add"})
             (actions/back-action))))))

(def role-fields ;; probably should be i18nized
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
  (layout/render ctx "Create Role"
                 (container/scrollbox (formui/dataform (render-role-fields)))
                 (actions/actions 
                   (actions/save-action {:method :post :url "/api/role/add"})
                   (actions/back-action))))

(defn get-view-role-edit
  [ctx]
  (if-let [role-id (-> ctx :query-params :role)]
    (let [role (service/get-role role-id)]
      (if (service/service-error? role)
        (ajax/error (meta role))
        (layout/render ctx "Edit Role"
                   (container/scrollbox (formui/dataform (render-role-fields role)))
                   (actions/actions 
                     (actions/save-action {:method :post :url "/api/role/edit" :params {:role role-id}})
                     (actions/delete-action {:url "/api/role" :params {:role role-id}})
                     (actions/back-action)))))))

(defn delete-api-role
  [ctx]
  (let [role (:role (:query-params ctx))
        resp (service/delete-role role)]
    (if (service/service-error? resp)
      (ajax/error (meta resp))
      (ajax/success resp))))

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

(defn add-roles-helper [roles locations assignee-type assignee-id]
  (let [mappings (if (or (nil? locations)      ;; Zero locations selected, add to all locations
                         (= 0 (count locations)))
                   (for [r roles]
                     {:role-id r :assignee-type (keyword assignee-type) :assignee-id assignee-id})
                   (for [r roles l locations]
                     {:role-id r :loc-id l :assignee-type (keyword assignee-type) :assignee-id assignee-id}))
        response-seq (doall (map service/add-rolemapping mappings))]  ;; force evaluation with doall
    (let [first-failure (some service/service-error? response-seq)]
      first-failure
      :success)))

(defn location-helper []
  (label-for-location nil (-> (sess/session-get :user) :organization)))

(defn post-api-role-assign
  [ctx]
  (let [{locations :location 
         roles :role } (:body-params ctx)
        {assignee-type :assignee-type
         assignee-id :assignee-id}  (:query-params ctx)]
    (cond
      (= 0 (count locations))
      (ajax/error {:message (format "Please select at least one %s." (location-helper))})
      (= (count roles) 0)
      (ajax/error {:message "Please select at least one role."})
      (and (< 1 (count locations))
           (some #{":all"} locations))
      (ajax/error {:message "Conflicting location option. Can't select all with other selections."})
      :else
      (let [locations (if (= [":all"] locations) nil locations)
            resp (add-roles-helper roles locations assignee-type assignee-id)]
          (if (not= :success resp)
            (ajax/error {:message "One or more roles not successfully assigned."})
            (ajax/success ""))))))

(defn rolechooser
  [{:keys [roles locations]}]
  (let [roles (for [x (sort-by :name roles)]
                {:value (:id x)
                 :label (:name x)})
        locations (cons
                    {:value ":all" :label (format "All %ss" (location-helper))}
                    (for [x (sort-by :name locations)]
                      {:value (:id x)
                       :label (:name x)}))]
                    (list
                      (formui/multiselect {:label "Roles" :name "role" :items roles})
                      (formui/multiselect {:label (str (location-helper) "s") :name "location" :items locations}))))

(defn get-view-roles-assign
  [ctx]
  (if-let [{qry-params :query-params} ctx] 
    (let [post-params (merge 
                        {:params (select-keys qry-params
                                              [:assignee-id :assignee-type])}
                        {:url "/api/role/assign"
                         :method :post})]
      (layout/render ctx "Assign Role"
                     (container/scrollbox (formui/dataform 
                                            (rolechooser {:roles (service/get-roles) :locations (service/get-locations)})))
                     (actions/actions 
                       (actions/save-action post-params)
                       (actions/back-action))))))

(defn ->friendly-name
  [rm]
  (if (:location rm)
    (format "%s in %s"
            (:name (:role rm))
            (:name (:location rm)))
    (:name (:role rm))))

(defn delete-api-rolemapping
  [{prms :query-params}]
  (let [resp (service/remove-rolemapping prms)]
    (if (service/service-error? resp)
      (ajax/error (meta resp))
      (ajax/success resp))))

(defn layout-group-effective-permissions 
  [grouproles]
  [:div.group-roles
   [:h3 "Group Inherited Roles"]
   (for [{:keys [group role location]} grouproles]
     [:div 
      (if location
        (format "%s in %s (%s)" (:name role) (:name location) (:name group))
        (format "%s (%s)" (:name role) (:name group)))])])

(defn get-view-roles-show
  [ctx]
  (if-let [{{:keys [assignee-id assignee-type]} :query-params} ctx]
    (let [assignee-type (keyword assignee-type)
          rolemappings (service/get-assigned-roles assignee-id assignee-type)
          delete-params {:params {:assignee-type assignee-type
                                  :assignee-id assignee-id
                                  :location :selected#location#id
                                  :role :selected#role#id}
                         :method :delete
                         :action-on-success "refresh"
                         :url "/api/rolemapping"}
          add-params {:url "/view/roles/assign"
                      :params {:assignee-type assignee-type
                               :assignee-id assignee-id}}]
      (if (service/service-error? rolemappings)
        (ajax/error (meta rolemappings))
        (layout/render ctx "Assigned Roles"
                       (container/scrollbox
                         (if (= :user assignee-type)
                           (list
                             (layout-group-effective-permissions (:group rolemappings))
                             [:h3 "User Roles"]))
                         (selectlist/selectlist {}
                                                (for [x (->> (assignee-type rolemappings)
                                                          (map #(assoc % :friendly-name 
                                                                       (->friendly-name %)))
                                                          (sort-by :friendly-name))]
                                                  {:label (:friendly-name x)
                                                   :data x})))
                       (actions/actions 
                         (actions/new-action add-params)
                         (actions/delete-action delete-params)
                         (actions/back-action)))))))

(def process-defns
  [{:name "get-view-roles"
    :runnable-fn (constantly true)
    :run-fn  layout-roles}
   {:name "get-view-role-add"
    :runnable-fn (constantly true)
    :run-fn get-view-role-add}
   {:name "get-view-roles-show"
    :runnable-fn (constantly true)
    :run-fn get-view-roles-show}
   {:name "get-view-roles-assign"
    :runnable-fn (constantly true)
    :run-fn get-view-roles-assign}
   {:name "delete-api-role"
    :runnable-fn (constantly true)
    :run-fn delete-api-role}
   {:name "delete-api-rolemapping"
    :runnable-fn (constantly true)
    :run-fn delete-api-rolemapping}
   {:name "get-view-role-edit"
    :runnable-fn (constantly true)
    :run-fn get-view-role-edit}
   {:name "post-api-role-edit"
    :runnable-fn (constantly true)
    :run-fn post-api-role-edit}
   {:name "post-api-role-assign"
    :runnable-fn (constantly true)
    :run-fn post-api-role-assign}
   {:name "post-api-role-add"
    :runnable-fn (constantly true)
    :run-fn post-api-role-add}
   ])

(process/register-processes (map #(DefaultProcess/create %) process-defns))
