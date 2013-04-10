;; Provides the configuration of the protocol managemant UIs.
(ns org.healthsciencessc.consent.commander.process.text-i18n
  (:require [org.healthsciencessc.consent.commander.ajax :as ajax]
            [org.healthsciencessc.consent.commander.security :as security]
            [org.healthsciencessc.consent.client.core :as services]
            [org.healthsciencessc.consent.common.roles :as roles]
            [org.healthsciencessc.consent.common.types :as types]
            
            [ring.util.response :as rutil]
            [pliant.webpoint.request :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))

(defn get-record
  [type id]
  (cond
    (= type types/meta-item) (services/get-meta-item id)
    (= type types/endorsement) (services/get-endorsement id)
    (= type types/policy) (services/get-policy id)
    (= type types/form) (services/get-form id)))

(defn- auth-on-text?
  [parent text-i18n-id property]
  (let [property-kw (keyword property)
        texts (property-kw parent)]
    (cond
      (map? texts) (= (:id texts) text-i18n-id)
      (coll? texts) (some #(= (:id %) text-i18n-id) texts))))


;; Register Create TextI18N Process
(defprocess create
  [ctx]
  (let [user (security/current-user ctx)
        parent-type (get-in ctx [:query-params :parent-type])
        parent-id (get-in ctx [:query-params :parent-id])
        property (get-in ctx [:query-params :property])
        parent (get-record parent-type parent-id)]
    (if (roles/can-design-org-id? user (get-in parent [:organization :id]))
      (let [body (:body-params ctx)
            resp (services/add-text-i18n parent-type parent-id property body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method create endpoint/endpoints "put-api-text-i18n")

;; Register Update TextI18N Process
(defprocess update
  [ctx]
  (let [user (security/current-user ctx)
        parent-type (get-in ctx [:query-params :parent-type])
        parent-id (get-in ctx [:query-params :parent-id])
        property (get-in ctx [:query-params :property])
        text-i18n-id (get-in ctx [:query-params :text-i18n])
        parent (get-record parent-type parent-id)]
    (if (and (roles/can-design-org-id? user (get-in parent [:organization :id])) 
             (auth-on-text? parent text-i18n-id property))
      (let [body (:body-params ctx)
            resp (services/update-text-i18n parent-type parent-id property text-i18n-id body)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method update endpoint/endpoints "post-api-text-i18n")

;; Register Delete TextI18N Process
(defprocess delete
  [ctx]
  (let [user (security/current-user ctx)
        parent-type (get-in ctx [:query-params :parent-type])
        parent-id (get-in ctx [:query-params :parent-id])
        property (get-in ctx [:query-params :property])
        text-i18n-id (get-in ctx [:query-params :text-i18n])
        parent (get-record parent-type parent-id)]
    (if (and (roles/can-design-org-id? user (get-in parent [:organization :id])) 
             (auth-on-text? parent text-i18n-id property))
      (let [resp (services/delete-text-i18n parent-type parent-id property text-i18n-id)]
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      (ajax/forbidden))))

(as-method delete endpoint/endpoints "delete-api-text-i18n")
