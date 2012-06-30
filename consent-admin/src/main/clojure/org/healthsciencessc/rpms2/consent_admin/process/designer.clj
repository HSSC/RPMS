;; Provides the configuration of the protocol managemant UIs.
(ns org.healthsciencessc.rpms2.consent-admin.process.designer
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
            [org.healthsciencessc.rpms2.consent-admin.ajax :as ajax]
            [org.healthsciencessc.rpms2.consent-admin.auth.protocol :as pauth]
            [org.healthsciencessc.rpms2.consent-admin.process.common :as common]
            [org.healthsciencessc.rpms2.consent-admin.process.protocol-version :as prot]
            [org.healthsciencessc.rpms2.consent-admin.services :as services]
            [org.healthsciencessc.rpms2.consent-admin.ui.container :as container]
            [org.healthsciencessc.rpms2.consent-admin.ui.layout :as layout]
            [ring.util.response :as rutil])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(defn view-protocol-version-designer
  "Generates a view of the form designer for a single protocol version"
  [ctx]
  (if-let [protocol-version-id (get-in ctx [:query-params :protocol-version])]
    (let [protocol-version (services/get-protocol-version protocol-version-id)
          protocol (:protocol protocol-version)
          editable (and (common/owned-by-user-org protocol)
                     (types/draft? protocol-version))]
      (if (meta protocol-version)
        (rutil/not-found (:message (meta protocol-version)))
        (layout/render ctx (prot/render-label protocol " Version - " (prot/version-name protocol-version))
                       (container/designer {:protocol-version protocol-version :editable editable
                                            :url "/api/protocol/version/form" :params {:protocol-version protocol-version-id}}))))
    (layout/render-error ctx {:message "A protocol version is required."})))

(defn post-api-form
  [ctx]
  (let [body (:body-params ctx)
        protocol-version-id (get-in ctx [:query-params :protocol-version])
        form-id (get-in ctx [:query-params :form])
        resp (services/update-designer-form protocol-version-id form-id body)]
    (if (services/service-error? resp)
        (ajax/save-failed (meta resp))
        (ajax/success resp))))

(defn put-api-widget
  [ctx]
  (let [body (:body-params ctx)
        protocol-version-id (get-in ctx [:query-params :protocol-version])
        widget-id (get-in ctx [:query-params :widget])
        form-id (get-in ctx [:query-params :form])
        resp (services/create-designer-form-widget protocol-version-id form-id widget-id body)]
    (if (services/service-error? resp)
        (ajax/save-failed (meta resp))
        (ajax/success resp))))

(defn post-api-widget
  [ctx]
  (let [body (:body-params ctx)
        protocol-version-id (get-in ctx [:query-params :protocol-version])
        widget-id (get-in ctx [:query-params :widget])
        resp (services/update-designer-form-widget protocol-version-id widget-id body)]
    (if (services/service-error? resp)
        (ajax/save-failed (meta resp))
        (ajax/success resp))))

(defn delete-api-widget
  [ctx]
  (let [protocol-version-id (get-in ctx [:query-params :protocol-version])
        widget-id (get-in ctx [:query-params :widget])
        resp (services/delete-designer-form-widget protocol-version-id widget-id)]
    (if (services/service-error? resp)
        (ajax/save-failed (meta resp))
        (ajax/success resp))))

(def process-defns
  [
   ;; Generates the view for editing/reviewing a protocol form.
   {:name "get-view-protocol-version-designer"
    :runnable-fn (fn [ctx] (pauth/auth-protocol-version-id (get-in ctx [:query-params :protocol-version])))
    :run-fn view-protocol-version-designer
    :run-if-false ajax/forbidden}
   
   ;; Service for updating a form
   {:name "post-api-form"
    :runnable-fn (fn [ctx] (pauth/auth-protocol-version-id (get-in ctx [:query-params :protocol-version])))
    :run-fn post-api-form
    :run-if-false ajax/forbidden}
   
   ;; Service for creating a widget
   {:name "put-api-widget"
    :runnable-fn (fn [ctx] (pauth/auth-protocol-version-id (get-in ctx [:query-params :protocol-version])))
    :run-fn put-api-widget
    :run-if-false ajax/forbidden}
   
   ;; Service for updating a widget
   {:name "post-api-widget"
    :runnable-fn (fn [ctx] (pauth/auth-protocol-version-id (get-in ctx [:query-params :protocol-version])))
    :run-fn post-api-widget
    :run-if-false ajax/forbidden}
   
   ;; Service for deleting a widget
   {:name "delete-api-widget"
    :runnable-fn (fn [ctx] (pauth/auth-protocol-version-id (get-in ctx [:query-params :protocol-version])))
    :run-fn delete-api-widget
    :run-if-false ajax/forbidden}
   ])

(process/register-processes (map #(DefaultProcess/create %) process-defns))
