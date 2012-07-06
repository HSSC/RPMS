;; Provides the configuration of the protocol managemant UIs.
(ns org.healthsciencessc.rpms2.consent-admin.process.protocol-version
  (:require [org.healthsciencessc.rpms2.consent-admin.auth.protocol :as pauth]
            [org.healthsciencessc.rpms2.consent-admin.security :as security]
            [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-admin.ui.layout :as layout]
            [org.healthsciencessc.rpms2.consent-admin.process.common :as common]
            [org.healthsciencessc.rpms2.consent-admin.services :as services]
            [org.healthsciencessc.rpms2.consent-admin.ui.container :as container]
            [org.healthsciencessc.rpms2.consent-admin.ui.actions :as actions]
            [org.healthsciencessc.rpms2.consent-admin.ui.selectlist :as selectlist]
            [org.healthsciencessc.rpms2.consent-admin.ui.form :as form]
            [org.healthsciencessc.rpms2.consent-admin.ajax :as ajax]
            [org.healthsciencessc.rpms2.consent-admin.download :as download]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
            [org.healthsciencessc.rpms2.consent-domain.tenancy :as tenancy]
            [org.healthsciencessc.rpms2.consent-admin.ui.common :as uicommon]
            [ring.util.response :as rutil])
  (:use [clojure.tools.logging :only (info error)])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

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

(defn view-protocol-versions
  "Generates a view that shows a list of the available versions of a protocol."
  [ctx]
  (if-let [protocol-id (get-in ctx [:query-params :protocol])]
    (let [versions (services/get-protocol-versions protocol-id)
          protocol (if (first versions) (:protocol (first versions)) (services/get-protocol protocol-id))
          label (render-label protocol " Version")]
       (if (meta versions)
        (rutil/not-found (:message (meta versions)))
        (layout/render ctx (render-label protocol " Versions")
                       (container/scrollbox 
                         (selectlist/selectlist {:action :.detail-action}
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
    (layout/render-error ctx {:message "A protocol parameter is required."})))

(defn view-protocol-version
  "Generates a detail/edit view for a single protocol version"
  [ctx]
  (if-let [protocol-version-id (get-in ctx [:query-params :protocol-version])]
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
                           (form/render-fields {} fields protocol-version))
                         (container/tabcontrol 
                           (uicommon/fill) 
                           (map (fn [{label :label options :options items :items}] 
                                  {:label label 
                                   :content (selectlist/actionlist (uicommon/fill-down options) items)} ) tubs)))
                       
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
                               {:method :post :url "/api/protocol/version/retire" :params {:protocol-version protocol-version-id}
                                :label "Retire" :action-on-success "refresh"
                                :confirm {:title "Confirm Retirement" :message "Retiring the current version will remove the protocol from the collection application."}})))
                         (actions/back-action)))))
    (layout/render-error ctx {:message "A protocol version is required."})))

(defn put-api-protocol-version
  "Adds a new protocol version to a protocol."
  [ctx]
  (if-let [protocol-id (get-in ctx [:query-params :protocol])]
    (let [versions (services/get-protocol-versions protocol-id)
          in-progress (filter #(or (types/draft? %) (types/submitted? %)) versions)]
      ;; Check If There Is A Version In Draft
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
                       :form {:name (str (:name protocol) " Layout")  ;; TODO - Change When create-records is fixed.
                              :titles [{:value [(:name protocol)] :language lang}]}}
              resp (services/add-protocol-version version)]
          ;; Handle Error or Success
          (if (services/service-error? resp)
            (ajax/save-failed (meta resp))
            (ajax/success resp)))
        
        ;; Handle Draft Already Existing
        (ajax/error {:message "A version already exists that is currently in a Draft or Submitted status. Delete the current in-progress version to create a new draft."})))
    ;; Handle Bad Request
    (ajax/error {:message "A protocol is required."})))

(defn delete-api-protocol-version
  "Deletes a specific version"
  [ctx]
  (if-let [protocol-version-id (get-in ctx [:query-params :protocol-version])]
    (let [resp (services/delete-protocol-version protocol-version-id)]
      ;; Handle Error or Success
      (if (services/service-error? resp)
        (ajax/save-failed (meta resp))
        (ajax/success resp)))
    ;; Handle Bad Request
    (ajax/error {:message "A protocol version is required."})))

(defn post-api-protocol-version-submit
  "Puts a draft protocol version into a submitted status."
  [ctx]
  (if-let [protocol-version-id (get-in ctx [:query-params :protocol-version])]
    (let [resp (services/update-protocol-version protocol-version-id {:status types/status-submitted})]
      ;; Handle Error or Success
      (if (services/service-error? resp)
        (ajax/save-failed (meta resp))
        (ajax/success resp)))
    ;; Handle Bad Request
    (ajax/error {:message "A protocol version is required."})))

(defn post-api-protocol-version-draft
  "Puts a submitted protocol version into a draft status."
  [ctx]
  (if-let [protocol-version-id (get-in ctx [:query-params :protocol-version])]
    (let [resp (services/draft-protocol-version protocol-version-id)]
      ;; Handle Error or Success
      (if (services/service-error? resp)
        (ajax/save-failed (meta resp))
        (ajax/success resp)))
    ;; Handle Bad Request
    (ajax/error {:message "A protocol version is required."})))

(defn post-api-protocol-version-publish
  "Publishes a specific protocol version"
  [ctx]
  (if-let [protocol-version-id (get-in ctx [:query-params :protocol-version])]
    (let [resp (services/publish-protocol-version protocol-version-id)]
      ;; Handle Error or Success
      (if (services/service-error? resp)
        (ajax/save-failed (meta resp))
        (ajax/success resp)))
    ;; Handle Bad Request
    (ajax/error {:message "A protocol version is required."})))

(defn post-api-protocol-version-retire
  "Publishes a specific protocol version"
  [ctx]
  (if-let [protocol-version-id (get-in ctx [:query-params :protocol-version])]
    (let [resp (services/retire-protocol-version protocol-version-id)]
      ;; Handle Error or Success
      (if (services/service-error? resp)
        (ajax/save-failed (meta resp))
        (ajax/success resp)))
    ;; Handle Bad Request
    (ajax/error {:message "A protocol version is required."})))

(defn delete-api-protocol-version-endorsement
  "Removes an endorsement from a protocol version."
  [ctx]
  (let [protocol-version-id (get-in ctx [:query-params :protocol-version])
        endorsement-id (get-in ctx [:query-params :endorsement])
        resp (services/remove-endorsement-from-protocol-version endorsement-id protocol-version-id)]
      ;; Handle Error or Success
      (if (services/service-error? resp)
        (ajax/save-failed (meta resp))
        (ajax/success resp))))

(defn delete-api-protocol-version-language
  "Removes a language from a protocol version."
  [ctx]
  (let [protocol-version-id (get-in ctx [:query-params :protocol-version])
        language-id (get-in ctx [:query-params :language])
        resp (services/remove-language-from-protocol-version language-id protocol-version-id)]
      ;; Handle Error or Success
      (if (services/service-error? resp)
        (ajax/save-failed (meta resp))
        (ajax/success resp))))

(defn delete-api-protocol-version-meta-item
  "Removes a meta item from a protocol version."
  [ctx]
  (let [protocol-version-id (get-in ctx [:query-params :protocol-version])
        meta-item-id (get-in ctx [:query-params :meta-item])
        resp (services/remove-meta-item-from-protocol-version meta-item-id protocol-version-id)]
      ;; Handle Error or Success
      (if (services/service-error? resp)
        (ajax/save-failed (meta resp))
        (ajax/success resp))))

(defn delete-api-protocol-version-policy
  "Removes a policy from a protocol version."
  [ctx]
  (let [protocol-version-id (get-in ctx [:query-params :protocol-version])
        policy-id (get-in ctx [:query-params :policy])
        resp (services/remove-policy-from-protocol-version policy-id protocol-version-id)]
      ;; Handle Error or Success
      (if (services/service-error? resp)
        (ajax/save-failed (meta resp))
        (ajax/success resp))))

(defn get-export-protocol-version
  "Puts a draft protocol version into a submitted status."
  [ctx]
  (if-let [protocol-version-id (get-in ctx [:query-params :protocol-version])]
    (let [resp (services/export-protocol-version protocol-version-id)]
      ;; Handle Error or Success
      (if (services/service-error? resp)
        (download/service-error (meta resp))
        (download/success-string resp "application/xml" "protocol.xml")))
    ;; Handle Bad Request
    (ajax/error {:message "A protocol version is required."})))

(def process-defns
  [
   ;; Generates the view for a specific protocol version.
   {:name "get-view-protocol-versions"
    :runnable-fn (fn [ctx] (pauth/auth-protocol-id (get-in ctx [:query-params :protocol])))
    :run-fn view-protocol-versions
    :run-if-false ajax/forbidden}
   
   ;; Service For Creating New Version
   {:name "put-api-protocol-version"
    :runnable-fn (fn [ctx] (pauth/auth-protocol-id (get-in ctx [:query-params :protocol])))
    :run-fn put-api-protocol-version
    :run-if-false ajax/forbidden}
   
   ;; Generates the view for editing/reviewing a protocol.
   {:name "get-view-protocol-version"
    :runnable-fn (fn [ctx] (pauth/auth-protocol-version-id (get-in ctx [:query-params :protocol-version])))
    :run-fn view-protocol-version
    :run-if-false ajax/forbidden}
   
   ;; Exports the Protocol Version xml file.
   {:name "get-export-protocol-version"
    :runnable-fn (fn [ctx] (pauth/auth-protocol-version-id (get-in ctx [:query-params :protocol-version])))
    :run-fn get-export-protocol-version
    :run-if-false ajax/forbidden}
   
   ;; Service For Deleting Protocol Version
   {:name "delete-api-protocol-version"
    :runnable-fn (fn [ctx] (pauth/auth-protocol-version-id (get-in ctx [:query-params :protocol-version]) types/draft?))
    :run-fn delete-api-protocol-version
    :run-if-false ajax/forbidden}
   
   ;; Service For Submitting Protocol Version
   {:name "post-api-protocol-version-submit"
    :runnable-fn (fn [ctx] (pauth/auth-protocol-version-id (get-in ctx [:query-params :protocol-version]) types/draft?))
    :run-fn post-api-protocol-version-submit
    :run-if-false ajax/forbidden}
   
   ;; Service For Reverting Protocol Version To Draft
   {:name "post-api-protocol-version-draft"
    :runnable-fn (fn [ctx] (pauth/auth-protocol-version-id (get-in ctx [:query-params :protocol-version]) types/submitted?))
    :run-fn post-api-protocol-version-draft
    :run-if-false ajax/forbidden}
   
   ;; Service For Publishing Protocol Version
   {:name "post-api-protocol-version-publish"
    :runnable-fn (fn [ctx] (pauth/auth-protocol-version-id (get-in ctx [:query-params :protocol-version]) types/submitted?))
    :run-fn post-api-protocol-version-publish
    :run-if-false ajax/forbidden}
   
   ;; Service For Publishing Protocol Version
   {:name "post-api-protocol-version-retire"
    :runnable-fn (fn [ctx] (pauth/auth-protocol-version-id (get-in ctx [:query-params :protocol-version]) types/published?))
    :run-fn post-api-protocol-version-retire
    :run-if-false ajax/forbidden}
   
   {:name "delete-api-protocol-version-endorsement"
    :runnable-fn (fn [ctx] (pauth/auth-protocol-version-id (get-in ctx [:query-params :protocol-version]) types/draft?))
    :run-fn delete-api-protocol-version-endorsement
    :run-if-false ajax/forbidden}
   
   {:name "delete-api-protocol-version-language"
    :runnable-fn (fn [ctx] (pauth/auth-protocol-version-id (get-in ctx [:query-params :protocol-version]) types/draft?))
    :run-fn delete-api-protocol-version-language
    :run-if-false ajax/forbidden}
   
   {:name "delete-api-protocol-version-meta-item"
    :runnable-fn (fn [ctx] (pauth/auth-protocol-version-id (get-in ctx [:query-params :protocol-version]) types/draft?))
    :run-fn delete-api-protocol-version-meta-item
    :run-if-false ajax/forbidden}
   
   {:name "delete-api-protocol-version-policy"
    :runnable-fn (fn [ctx] (pauth/auth-protocol-version-id (get-in ctx [:query-params :protocol-version]) types/draft?))
    :run-fn delete-api-protocol-version-policy
    :run-if-false ajax/forbidden}
   ])

(process/register-processes (map #(DefaultProcess/create %) process-defns))
