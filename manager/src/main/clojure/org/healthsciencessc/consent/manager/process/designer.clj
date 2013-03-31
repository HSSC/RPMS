;; Provides the configuration of the protocol managemant UIs.
(ns org.healthsciencessc.consent.manager.process.designer
  (:require [org.healthsciencessc.consent.manager.ajax :as ajax]
            [org.healthsciencessc.consent.client.core :as services]
            
            [org.healthsciencessc.consent.manager.process.common :as common]
            [org.healthsciencessc.consent.manager.process.protocol-version :as prot]
            
            [org.healthsciencessc.consent.manager.ui.container :as container]
            [org.healthsciencessc.consent.manager.ui.layout :as layout]
            
            [org.healthsciencessc.consent.domain.lookup :as lookup]
            [org.healthsciencessc.consent.domain.types :as types]
            
            [ring.util.response :as rutil]
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))


;; Register View Protocol Version Form Designer Process
(defprocess view-designer
  "Generates a view of the form designer for a single protocol version"
  [ctx]
  (if-let [protocol-version-id (lookup/get-protocol-version-in-query ctx)]
    (if (prot/auth-protocol-version-id protocol-version-id)
          (let [protocol-version (services/get-protocol-version protocol-version-id)
          protocol (:protocol protocol-version)
          editable (and (common/owned-by-user-org protocol)
                     (types/draft? protocol-version))]
      (if (meta protocol-version)
        (rutil/not-found (:message (meta protocol-version)))
        (layout/render ctx (prot/render-label protocol " Version - " (prot/version-name protocol-version))
                       (container/designer {:protocol-version protocol-version 
                                            :editable editable
                                            :url "/api/protocol/version/form" 
                                            :params {:protocol-version protocol-version-id}}))))
      (ajax/forbidden))
    (layout/render-error ctx {:message "A protocol version is required."})))

(as-method view-designer endpoint/endpoints "get-view-protocol-version-designer")

;; Register Update Form Process
(defprocess update-form
  [ctx]
  (let [protocol-version-id (lookup/get-protocol-version-in-query ctx)]
    (if (prot/auth-protocol-version-id protocol-version-id  types/draft?)
      (let [body (:body-params ctx)
            form-id (lookup/get-form-in-query ctx)
            resp (services/update-designer-form protocol-version-id form-id body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method update-form endpoint/endpoints "post-api-form")


;; Register Create Widget Process
(defprocess create
  [ctx]
  (let [protocol-version-id (lookup/get-protocol-version-in-query ctx)]
    (if (prot/auth-protocol-version-id protocol-version-id  types/draft?)
      (let [body (:body-params ctx)
            widget-id (lookup/get-widget-in-query ctx)
            form-id (lookup/get-form-in-query ctx)
            resp (services/create-designer-form-widget protocol-version-id form-id widget-id body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method create endpoint/endpoints "put-api-widget")


;; Register Update Widget Process
(defprocess update
  [ctx]
  (let [protocol-version-id (lookup/get-protocol-version-in-query ctx)]
    (if (prot/auth-protocol-version-id protocol-version-id  types/draft?)
      (let [body (:body-params ctx)
            widget-id (lookup/get-widget-in-query ctx)
            resp (services/update-designer-form-widget protocol-version-id widget-id body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method update endpoint/endpoints "post-api-widget")

;; Register Update Widget Process
(defprocess delete
  [ctx]
  (let [protocol-version-id (lookup/get-protocol-version-in-query ctx)]
    (if (prot/auth-protocol-version-id protocol-version-id  types/draft?)
      (let [widget-id (lookup/get-widget-in-query ctx)
            resp (services/delete-designer-form-widget protocol-version-id widget-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method delete endpoint/endpoints "delete-api-widget")
