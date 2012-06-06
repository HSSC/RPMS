;; Provides the configuration of the protocol managemant UIs.
(ns org.healthsciencessc.rpms2.consent-admin.process.protocol-version
  (:require [org.healthsciencessc.rpms2.consent-admin.auth.protocol :as pauth]
            [org.healthsciencessc.rpms2.consent-admin.security :as security]
            [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-admin.ui.layout :as layout]
            [org.healthsciencessc.rpms2.consent-admin.services :as services]
            [org.healthsciencessc.rpms2.consent-admin.ui.container :as container]
            [org.healthsciencessc.rpms2.consent-admin.ui.actions :as actions]
            [org.healthsciencessc.rpms2.consent-admin.ui.selectlist :as selectlist]
            [org.healthsciencessc.rpms2.consent-admin.ui.form :as form]
            [org.healthsciencessc.rpms2.consent-admin.ajax :as ajax]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
            [org.healthsciencessc.rpms2.consent-domain.tenancy :as tenancy]
            [ring.util.response :as rutil])
  (:use [clojure.tools.logging :only (info error)])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def ^:const fields [{:name :version :label "Version"}
                     {:name :status :label "Status"}])

(defn- render-label
  [protocol & addons]
  (let [org (security/current-org)
        location (:location protocol)
        label (tenancy/label-for-protocol location org)]
    (str label (apply str addons))))

(defn- version-name
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
          protocol (if (first versions) (:protocol (first versions)) (services/get-protocol protocol-id))]
       (if (meta versions)
        (rutil/not-found (:message (meta versions)))
        (layout/render ctx (render-label protocol " Versions")
                       (container/scrollbox 
                         (selectlist/selectlist
                           (for [version versions]
                             {:label (str "Version " (version-name version))
                              :data version})))
                       (actions/actions 
                         (actions/push-action 
                           {:url "/view/protocol/version" :params {:version :selected#id} 
                            :label "Details/Edit"})
                         (actions/ajax-action 
                           {:method :put :url "/api/protocol/version" :params {:protocol protocol-id}
                            :label "New" :action-on-success "refresh"})
                         (actions/back-action)))))
    (layout/render-error ctx {:message "A protocol is required."})))

(defn view-protocol-version
  "Generates a detail/edit view for a single protocol version"
  [ctx]
  (if-let [protocol-version-id (get-in ctx [:query-params :version])]
    (let [protocol-version (services/get-protocol-version protocol-version-id)
          protocol (:protocol protocol-version)]
       (if (meta protocol-version)
        (rutil/not-found (:message (meta protocol-version)))
        (layout/render ctx (render-label protocol " Version - " (version-name protocol-version))
                       (container/scrollbox 
                         )
                       (actions/actions         
                         (if (types/draft? protocol-version)
                           (list
                             (actions/push-action 
                               {:url "/view/protocol/version/policies" :params {:version protocol-version-id} 
                                :label "Edit Policies"})
                             (actions/push-action 
                               {:url "/view/protocol/version/endorsements" :params {:version protocol-version-id} 
                                :label "Edit Endorsements"})
                             (actions/push-action 
                               {:url "/view/protocol/version/metaitem" :params {:version protocol-version-id} 
                                :label "Edit Meta Items"})
                             (actions/push-action 
                               {:url "/view/protocol/version/form" :params {:version protocol-version-id} 
                                :label "Edit Form"})
                             
                             (actions/action-separator)
                             
                             (actions/ajax-action 
                               {:method :delete :url "/api/protocol/version" :params {:version protocol-version-id}
                                :label "Delete" :action-on-success ".back-action"})
                             (actions/ajax-action 
                               {:method :post :url "/api/protocol/version/publish" :params {:version protocol-version-id}
                                :label "Publish" :action-on-success "refresh"
                                :confirm {:title "Confirm Publishing" :message "Publishing the current version will retire any currently published versions."}})))
                         (if (types/published? protocol-version)
                           (actions/ajax-action 
                             {:method :post :url "/api/protocol/version/retire" :params {:version protocol-version-id}
                              :label "Retire" :action-on-success "refresh"
                              :confirm {:title "Confirm Retirement" :message "Retiring the current version will remove the protocol from the collection application."}}))
                         (actions/back-action)))))
    (layout/render-error ctx {:message "A protocol version is required."})))

(defn put-api-protocol-version
  "Adds a new protocol version to a protocol."
  [ctx]
  (if-let [protocol-id (get-in ctx [:query-params :protocol])]
    (let [versions (services/get-protocol-versions protocol-id)
          drafts (filter #(= "Draft" (:status %)) versions)]
      ;; Check If There Is A Version In Draft
      (if (empty? drafts)
        (let [protocol (services/get-protocol protocol-id)
              last-version (or (:last-version protocol) (count versions))
              version {:version (+ 1 last-version)
                       :protocol protocol
                       :location (:location protocol)
                       :organization (:organization protocol)}
              resp (services/add-protocol-version version)]
          (info "Creating new version" version)
          (info "Version Response" resp)
              
          ;; Handle Error or Success
          (if (services/service-error? resp)
            (ajax/save-failed (meta resp))
            (ajax/success resp)))
        
        ;; Handle Draft Already Existing
        (ajax/error {:message "A 'Draft' version already exists. Either delete or retire the current drafted version to create a new draft."})))
    ;; Handle Bad Request
    (ajax/error {:message "A protocol is required."})))

(defn delete-api-protocol-version
  "Deletes a specific version"
  [ctx]
  (if-let [protocol-version-id (get-in ctx [:query-params :version])]
    (let [resp (services/delete-protocol-version protocol-version-id)]
      ;; Handle Error or Success
      (if (services/service-error? resp)
        (ajax/save-failed (meta resp))
        (ajax/success resp)))
    ;; Handle Bad Request
    (ajax/error {:message "A protocol version is required."})))

(defn post-api-protocol-version-publish
  "Publishes a specific protocol version"
  [ctx]
  (if-let [protocol-version-id (get-in ctx [:query-params :version])]
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
  (if-let [protocol-version-id (get-in ctx [:query-params :version])]
    (let [resp (services/retire-protocol-version protocol-version-id)]
      ;; Handle Error or Success
      (if (services/service-error? resp)
        (ajax/save-failed (meta resp))
        (ajax/success resp)))
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
    :runnable-fn (fn [ctx] (pauth/auth-protocol-version-id (get-in ctx [:query-params :version])))
    :run-fn view-protocol-version
    :run-if-false ajax/forbidden}
   
   ;; Service For Deleting Protocol Version
   {:name "delete-api-protocol-version"
    :runnable-fn (fn [ctx] (pauth/auth-protocol-version-id (get-in ctx [:query-params :version]) types/draft?))
    :run-fn delete-api-protocol-version
    :run-if-false ajax/forbidden}
   
   ;; Service For Publishing Protocol Version
   {:name "post-api-protocol-version-publish"
    :runnable-fn (fn [ctx] (pauth/auth-protocol-version-id (get-in ctx [:query-params :version]) types/draft?))
    :run-fn post-api-protocol-version-publish
    :run-if-false ajax/forbidden}
   
   ;; Service For Publishing Protocol Version
   {:name "post-api-protocol-version-retire"
    :runnable-fn (fn [ctx] (pauth/auth-protocol-version-id (get-in ctx [:query-params :version]) types/published?))
    :run-fn post-api-protocol-version-retire
    :run-if-false ajax/forbidden}
   ])

(process/register-processes (map #(DefaultProcess/create %) process-defns))
