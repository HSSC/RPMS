;; Provides the configuration of the protocol managemant UIs.
(ns org.healthsciencessc.rpms2.consent-admin.process.protocol-version
  (:require [org.healthsciencessc.rpms2.consent-admin.ajax :as ajax]
            [org.healthsciencessc.rpms2.consent-admin.download :as download]
            [org.healthsciencessc.rpms2.consent-admin.security :as security]
            [org.healthsciencessc.rpms2.consent-admin.services :as services]
            
            [org.healthsciencessc.rpms2.consent-admin.process.common :as common]
            
            [org.healthsciencessc.rpms2.consent-admin.ui.actions :as actions]
            [org.healthsciencessc.rpms2.consent-admin.ui.common :as uicommon]
            [org.healthsciencessc.rpms2.consent-admin.ui.container :as container]
            [org.healthsciencessc.rpms2.consent-admin.ui.form :as form]
            [org.healthsciencessc.rpms2.consent-admin.ui.layout :as layout]
            [org.healthsciencessc.rpms2.consent-admin.ui.list :as list]
            
            [org.healthsciencessc.rpms2.consent-domain.lookup :as lookup]
            [org.healthsciencessc.rpms2.consent-domain.roles :as roles]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
            [org.healthsciencessc.rpms2.consent-domain.tenancy :as tenancy]
            
            [ring.util.response :as rutil]
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))


(defn auth-protocol-version-id
  [protocol-version-id & contraints]
  (let [protocol-version (services/get-protocol-version protocol-version-id)
        protocol (:protocol protocol-version)
        user (security/current-user)]
    (and (roles/can-design-protocol? user protocol)
         (every? #(% protocol-version) contraints))))

(def fields [{:name :version :label "Version"}
             {:name :status :label "Status" :readonly true}])

(defn render-label
  [protocol & addons]
  (let [org (security/current-org)
        location (:location protocol)
        label (tenancy/label-for-protocol location org)]
    (str label (apply str addons))))

(defn version-name
  [version]
  (str (:version version) " [" (:status version) "]"))

(defn- version-full-name
  [version]
  (str (get-in version [:protocol :name]) " - Version " (:version version) " [" (:status version) "]"))


;; Register View Protocol Versions Process
(defprocess view-protocol-versions
  "Generates a view that shows a list of the available versions of a protocol."
  [ctx]
  (if-let [protocol-id (lookup/get-protocol-in-query ctx)]
    (let [user (security/current-user ctx)
          protocol (services/get-protocol protocol-id)
          location (:location protocol)
          location-role (first (roles/protocol-designer-mappings user :location location))]
      (if location-role
        (let [versions (services/get-protocol-versions protocol-id)
              label (render-label protocol " Version")]
          (if (meta versions)
            (rutil/not-found (:message (meta versions)))
            (layout/render ctx (render-label protocol " Versions")
                           (container/scrollbox 
                             (list/selectlist {:action :.detail-action}
                                              (for [version versions]
                                                {:label (str "Version " (version-name version))
                                                 :data version})))
                           (actions/actions 
                             (actions/details-action 
                               {:url "/view/protocol/version" :params {:protocol-version :selected#id}
                                :verify (actions/gen-verify-a-selected label)})
                             (actions/ajax-action 
                               {:method :put :url "/api/protocol/version" :params {:protocol protocol-id}
                                :label "New" :action-on-success "refresh"})
                             (actions/back-action)))))
        (ajax/forbidden)))
    (layout/render-error ctx {:message "A protocol is required."})))

(as-method view-protocol-versions endpoint/endpoints "get-view-protocol-versions")


;; Register Update Protocol Version Process
(defprocess view-protocol-version
  "Generates a detail/edit view for a single protocol version"
  [ctx]
  (let [protocol-version-id (lookup/get-protocol-version-in-query ctx)]
    (if (auth-protocol-version-id protocol-version-id)
      (let [protocol-version (services/get-protocol-version protocol-version-id)
            protocol (:protocol protocol-version)
            editable (and (common/owned-by-user-org protocol)
                          (types/draft? protocol-version))
            params {:protocol-version protocol-version-id}
            
            tabs [{:label "Policies" :type "policy" :items (:policies protocol-version) :edit false}
                  {:label "Endorsements" :type "endorsement" :items (:endorsements protocol-version) :edit false}
                  {:label "Meta Items" :type "meta-item" :items (:meta-items protocol-version) :edit false}
                  {:label "Languages" :type "language" :items (:languages protocol-version) :edit false}]
            tubs (into [] 
                       (for [tab tabs] 
                         (let [edit-options (if (not (false? (:edit tab))) 
                                              {:edit-url (str "/view/protocol/version/" (:type tab) "/edit") :edit-params params} {})]
                           {:label (:label tab)
                            :options (merge {:editable editable
                                             :add-url (str "/view/protocol/version/" (:type tab) "/add") :add-params params
                                             :delete-url (str "/api/protocol/version/" (:type tab)) :delete-params params} edit-options)
                            :items (map (fn [item] {:label (:name item) :data {(:type tab) (:id item)}}) (:items tab))})))]
        (if (meta protocol-version)
          (rutil/not-found (:message (meta protocol-version)))
          (layout/render ctx (render-label protocol " Version - " (version-name protocol-version))
                         (container/cutbox
                           (form/dataform
                             (form/render-fields {:editable editable} fields protocol-version))
                           (container/tabcontrol 
                             (uicommon/fill) 
                             (map (fn [{label :label options :options items :items}] 
                                    {:label label 
                                     :content (list/actionlist (uicommon/fill-down options) items)} ) tubs)))
                         
                         (actions/actions         
                           (if (types/draft? protocol-version)
                             (list
                               (actions/push-action 
                                 {:url "/view/protocol/version/designer" :params {:protocol-version protocol-version-id} 
                                  :label "Layout"})
                               (actions/ajax-action 
                                 {:method :post :url "/api/protocol/version/submit" :params {:protocol-version protocol-version-id}
                                  :label "Submit" :action-on-success "refresh"})
                               (actions/ajax-action 
                                 {:method :delete :url "/api/protocol/version" :params {:protocol-version protocol-version-id}
                                  :label "Delete" :action-on-success ".back-action"})))
                           
                           (if (types/submitted? protocol-version)
                             (list
                               (actions/push-action 
                                 {:url "/view/protocol/version/designer" :params {:protocol-version protocol-version-id} 
                                  :label "Review Layout"})
                               (actions/open-action 
                                 {:url "/export/protocol/version" :params {:protocol-version protocol-version-id}
                                  :label "Export" })
                               (actions/ajax-action 
                                 {:method :post :url "/api/protocol/version/draft" :params {:protocol-version protocol-version-id}
                                  :label "Revert To Draft" :action-on-success "refresh"})
                               (actions/ajax-action 
                                 {:method :post :url "/api/protocol/version/publish" :params {:protocol-version protocol-version-id}
                                  :label "Publish" :action-on-success "refresh"
                                  :confirm {:title "Confirm Publishing" :message "Publishing the current version will retire any currently published versions."}})))
                           
                           (if (types/published? protocol-version)
                             (list
                               (actions/push-action 
                                 {:url "/view/protocol/version/designer" :params {:protocol-version protocol-version-id} 
                                  :label "View Layout"})
                               (actions/open-action 
                                 {:url "/export/protocol/version" :params {:protocol-version protocol-version-id}
                                  :label "Export" })
                               (actions/ajax-action 
                                 {:method :post :url "/api/protocol/version/clone" :params {:protocol-version protocol-version-id}
                                  :label "Clone"
                                  :action-on-success ".back-action"})
                               (actions/ajax-action 
                                 {:method :post :url "/api/protocol/version/retire" :params {:protocol-version protocol-version-id}
                                  :label "Retire" :action-on-success "refresh"
                                  :confirm {:title "Confirm Retirement" :message "Retiring the current version will remove the protocol from the collection application."}})))
                           (actions/back-action)))))
        (ajax/forbidden))))

(as-method view-protocol-version endpoint/endpoints "get-view-protocol-version")


;; Register Export Protocol Version Process
(defprocess export
  "Exports a protocol version data into an XML format to use for creating documentation."
  [ctx]
  (let [protocol-version-id (lookup/get-protocol-version-in-query ctx)]
    (if (auth-protocol-version-id protocol-version-id)
      (let [resp (services/export-protocol-version protocol-version-id)]
        (if (services/service-error? resp)
          (download/service-error (meta resp))
          (download/success-string resp "application/xml" "protocol.xml")))
      (ajax/forbidden))))

(as-method export endpoint/endpoints "get-export-protocol-version")

;; Register Delete Protocol Version Endorsement Process
(defprocess delete-endorsement
  "Removes a endorsement from a protocol version."
  [ctx]
  (let [protocol-version-id (lookup/get-protocol-version-in-query ctx)]
    (if (auth-protocol-version-id protocol-version-id types/draft?)
      (let [endorsement-id (lookup/get-endorsement-in-query ctx)
            resp (services/remove-endorsement-from-protocol-version endorsement-id protocol-version-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method delete-endorsement endpoint/endpoints "delete-api-protocol-version-endorsement")

;; Register Delete Protocol Version Language Process
(defprocess delete-language
  "Removes a language from a protocol version."
  [ctx]
  (let [protocol-version-id (lookup/get-protocol-version-in-query ctx)]
    (if (auth-protocol-version-id protocol-version-id types/draft?)
      (let [language-id (lookup/get-language-in-query ctx)
            resp (services/remove-language-from-protocol-version language-id protocol-version-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method delete-language endpoint/endpoints "delete-api-protocol-version-language")

;; Register Delete Protocol Version Meta Item Process
(defprocess delete-meta-item
  "Removes a meta item from a protocol version."
  [ctx]
  (let [protocol-version-id (lookup/get-protocol-version-in-query ctx)]
    (if (auth-protocol-version-id protocol-version-id types/draft?)
      (let [meta-item-id (lookup/get-meta-item-in-query ctx)
            resp (services/remove-meta-item-from-protocol-version meta-item-id protocol-version-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method delete-meta-item endpoint/endpoints "delete-api-protocol-version-meta-item")

;; Register Delete Protocol Version Policy Process
(defprocess delete-policy
  "Removes a policy from a protocol version."
  [ctx]
  (let [protocol-version-id (lookup/get-protocol-version-in-query ctx)]
    (if (auth-protocol-version-id protocol-version-id types/draft?)
      (let [policy-id (lookup/get-policy-in-query ctx)
            resp (services/remove-policy-from-protocol-version policy-id protocol-version-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method delete-policy endpoint/endpoints "delete-api-protocol-version-policy")

;; Register Submit Protocol Version Process
(defprocess submit
  [ctx]
  (let [protocol-version-id (lookup/get-protocol-version-in-query ctx)]
    (if (auth-protocol-version-id protocol-version-id types/draft?)
      (let [resp (services/submit-protocol-version protocol-version-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method submit endpoint/endpoints "post-api-protocol-version-submit")

;; Register Draft Protocol Version Process
(defprocess draft
  [ctx]
  (let [protocol-version-id (lookup/get-protocol-version-in-query ctx)]
    (if (auth-protocol-version-id protocol-version-id types/submitted?)
      (let [resp (services/draft-protocol-version protocol-version-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method draft endpoint/endpoints "post-api-protocol-version-draft")

;; Register Publish Protocol Version Process
(defprocess publish
  [ctx]
  (let [protocol-version-id (lookup/get-protocol-version-in-query ctx)]
    (if (auth-protocol-version-id protocol-version-id types/submitted?)
      (let [resp (services/publish-protocol-version protocol-version-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method publish endpoint/endpoints "post-api-protocol-version-publish")

;; Register Retire Protocol Version Process
(defprocess retire
  [ctx]
  (let [protocol-version-id (lookup/get-protocol-version-in-query ctx)]
    (if (auth-protocol-version-id protocol-version-id types/published?)
      (let [resp (services/retire-protocol-version protocol-version-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method retire endpoint/endpoints "post-api-protocol-version-retire")

;; Register Clone Protocol Version Process
(defprocess clone
  [ctx]
  (let [protocol-version-id (lookup/get-protocol-version-in-query ctx)]
    (if (auth-protocol-version-id protocol-version-id types/published?)
      (let [resp (services/clone-protocol-version protocol-version-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method clone endpoint/endpoints "post-api-protocol-version-clone")

;; Register Create Protocol Process
(defprocess create
  [ctx]
  (if-let [protocol-id (lookup/get-protocol-in-query ctx)]
    (let [user (security/current-user ctx)
          protocol (services/get-protocol protocol-id)
          location (:location protocol)
          location-role (first (roles/protocol-designer-mappings user :location location))]
      (if location-role
        (let [versions (services/get-protocol-versions protocol-id)
              in-progress (filter #(or (types/draft? %) (types/submitted? %)) versions)]
          (if (empty? in-progress)
            (let [protocol (services/get-protocol protocol-id)
                  last-version (or (:last-version protocol) (count versions))
                  lang (or (get-in protocol [:organization :language])
                           (first (filter #(= "en" (:code %)) (services/get-languages))))
                  version {:version (+ 1 last-version)
                           :protocol protocol
                           :location (:location protocol)
                           :organization (:organization protocol)
                           :languages [lang] ;; TODO - Change When create-records is fixed.
                           :form {:name (str (:name protocol) " Layout")
                                  :organization (:organization protocol);; TODO - Change When create-records is fixed.
                                  :titles [{:value [(:name protocol)] :language lang}]}}
                  resp (services/add-protocol-version version)]
              ;; Handle Error or Success
              (if (services/service-error? resp)
                (ajax/save-failed (meta resp))
                (ajax/success resp)))
            
            ;; Handle Draft Already Existing
            (ajax/error {:message "A version already exists that is currently in a Draft or Submitted status. Delete the current in-progress version to create a new draft."})))
          (ajax/forbidden)))
      (ajax/error {:message "A protocol is required."})))

(as-method create endpoint/endpoints "put-api-protocol-version")

;; Register Update Protocol Version Process
(defprocess update
  [ctx]
  (let [protocol-version-id (lookup/get-protocol-version-in-query ctx)]
    (if (auth-protocol-version-id protocol-version-id  types/draft?)
      (let [body (:body-params ctx)
            resp (services/update-protocol-version protocol-version-id body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method update endpoint/endpoints "post-api-protocol-version")

;; Register Update Protocol Version Process
(defprocess delete
  [ctx]
  (let [protocol-version-id (lookup/get-protocol-version-in-query ctx)]
    (if (auth-protocol-version-id protocol-version-id  types/draft?)
      (let [resp (services/delete-protocol-version protocol-version-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method delete endpoint/endpoints "delete-api-protocol-version")
