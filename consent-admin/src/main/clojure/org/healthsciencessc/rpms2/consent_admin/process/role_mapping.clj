(ns org.healthsciencessc.rpms2.consent-admin.process.role-mapping
  (:require [org.healthsciencessc.rpms2.consent-admin.ajax :as ajax]
            [org.healthsciencessc.rpms2.consent-admin.security :as security]
            [org.healthsciencessc.rpms2.consent-admin.services :as services]
            
            [org.healthsciencessc.rpms2.consent-admin.process.common :as common]
            
            [org.healthsciencessc.rpms2.consent-admin.ui.actions :as actions]
            [org.healthsciencessc.rpms2.consent-admin.ui.container :as container]
            [org.healthsciencessc.rpms2.consent-admin.ui.form :as form]
            [org.healthsciencessc.rpms2.consent-admin.ui.layout :as layout]
            [org.healthsciencessc.rpms2.consent-admin.ui.list :as list]
            
            [org.healthsciencessc.rpms2.consent-domain.lookup :as lookup]
            [org.healthsciencessc.rpms2.consent-domain.runnable :as runnable]
            [org.healthsciencessc.rpms2.consent-domain.tenancy :as tenancy]
        
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))

(defn add-roles-helper [roles locations assignee-type assignee-id]
  (let [mappings (if (or (nil? locations)      ;; Zero locations selected, add to all locations
                         (= 0 (count locations)))
                   (for [r roles]
                     {:role-id r :assignee-type (keyword assignee-type) :assignee-id assignee-id})
                   (for [r roles l locations]
                     {:role-id r :loc-id l :assignee-type (keyword assignee-type) :assignee-id assignee-id}))
        response-seq (doall (map services/add-rolemapping mappings))]  ;; force evaluation with doall
    (let [first-failure (some services/service-error? response-seq)]
      first-failure
      :success)))

;; Define Processes For Assigning Roles To Users and Groups

(defn location-helper []
  (tenancy/label-for-location nil (security/current-org)))

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
                      (form/multiselect {:label "Roles" :name "role" :items roles})
                      (form/multiselect {:label (str (location-helper) "s") :name "location" :items locations}))))

;; Register View Assign Role Process
(defprocess view-assign-roles
  "Generates a view of all the roles a user has currently assigned to them."
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (runnable/can-admin-org-id user org-id)
      (if-let [{qry-params :query-params} ctx] 
        (let [post-params (merge 
                            {:params (select-keys qry-params
                                                  [:assignee-id :assignee-type])}
                            {:url "/api/role/assign"
                             :method :post})]
          (layout/render ctx "Assign Role"
                         (container/scrollbox (form/dataform 
                                                (rolechooser {:roles (services/get-roles) :locations (services/get-locations)})))
                         (actions/actions 
                           (actions/save-action post-params)
                           (actions/back-action))))
        (layout/render-error ctx {:message "An assignee type and id are required."}))
      (ajax/forbidden))))

(as-method view-assign-roles endpoint/endpoints "get-view-roles-assign")


(defn ->friendly-name
  [rm]
  (if (:location rm)
    (format "%s in %s"
            (:name (:role rm))
            (:name (:location rm)))
    (:name (:role rm))))

(defn layout-group-effective-permissions 
  [grouproles]
  [:div.group-roles
   [:h3 "Group Inherited Roles"]
   (for [{:keys [group role location]} grouproles]
     [:div 
      (if location
        (format "%s in %s (%s)" (:name role) (:name location) (:name group))
        (format "%s (%s)" (:name role) (:name group)))])])

;; Register View Assigned Roles Process
(defprocess view-assigned-roles
  "Generates a view of all the roles a user has currently assigned to them."
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (runnable/can-admin-org-id user org-id)
      (if-let [{{:keys [assignee-id assignee-type]} :query-params} ctx]
        (let [assignee-type (keyword assignee-type)
              rolemappings (services/get-assigned-roles assignee-id assignee-type)
              delete-params {:params {:role-mapping :selected#id}
                             :method :delete
                             :action-on-success "refresh"
                             :url "/api/role-mapping"}
              add-params {:url "/view/roles/assign"
                          :params {:assignee-type assignee-type
                                   :assignee-id assignee-id}}]
          (if (services/service-error? rolemappings)
            (ajax/error (meta rolemappings))
            (layout/render ctx "Assigned Roles"
                           (container/scrollbox
                             (if (= :user assignee-type)
                               (list
                                 (layout-group-effective-permissions (:group rolemappings))
                                 [:h3 "User Roles"]))
                             (list/selectlist {}
                                              (for [x (->> (assignee-type rolemappings)
                                                        (map #(assoc % :friendly-name 
                                                                     (->friendly-name %)))
                                                        (sort-by :friendly-name))]
                                                {:label (:friendly-name x)
                                                 :data (select-keys x [:id])})))
                           (actions/actions 
                             (actions/new-action add-params)
                             (actions/delete-action delete-params)
                             (actions/back-action)))))
        (layout/render-error ctx {:message "An assignee type and id are required."}))
      (ajax/forbidden))))

(as-method view-assigned-roles endpoint/endpoints "get-view-roles-show")

;; Register Assign Role Mapping Process
(defprocess assign-role-mapping
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (runnable/can-admin-org-id user org-id)
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
              (ajax/success "")))))
      (ajax/forbidden))))

(as-method assign-role-mapping endpoint/endpoints "post-api-role-assign")
    
;; Register Delete Role Mapping Process
(defprocess delete-role-mapping
  [ctx]
  (let [user (security/current-user ctx)
        org-id (common/lookup-organization ctx)]
    (if (runnable/can-admin-org-id user org-id)
      (let [role-mapping-id (lookup/get-role-mapping-in-query ctx)
            resp (services/delete-role-mapping role-mapping-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method delete-role-mapping endpoint/endpoints "delete-api-role-mapping")
