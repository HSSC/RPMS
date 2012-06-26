;; Provides the configuration of the protocol managemant UIs.
(ns org.healthsciencessc.rpms2.consent-admin.process.text-i18n
  (:require [org.healthsciencessc.rpms2.consent-admin.ajax :as ajax]
            [org.healthsciencessc.rpms2.consent-admin.security :as security]
            [org.healthsciencessc.rpms2.consent-admin.services :as services]
            [org.healthsciencessc.rpms2.consent-domain.runnable :as runnable]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
            [org.healthsciencessc.rpms2.process-engine.core :as process]
            [ring.util.response :as rutil])
  (:use [clojure.tools.logging :only (info error)])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(defn get-record
  [type id]
  (cond
    (= type types/meta-item) (services/get-meta-item id)
    (= type types/endorsement) (services/get-endorsement id)
    (= type types/policy) (services/get-policy id)
    (= type types/form) (services/get-form id)))

(defn- auth-on-parent-type
  [ctx]
  (let [parent-type (get-in ctx [:query-params :parent-type])
        parent-id (get-in ctx [:query-params :parent-id])
        parent (get-record parent-type parent-id)]
    (if (runnable/can-design-org-id (security/current-user ctx) (get-in parent [:organization :id]))
      parent)))

(defn- auth-on-parent-type-text
  [ctx]
  (let [parent (auth-on-parent-type ctx)]
    (if parent
      (let [text-i18n-id (get-in ctx [:query-params :text-i18n])
            property (keyword (get-in ctx [:query-params :property]))
            texts (property parent)]
        (cond
          (map? texts) (= (:id texts) text-i18n-id)
          (coll? texts) (some #(= (:id %) text-i18n-id) texts))))))

(defn add-text
  [ctx]
  (let [parent-type (get-in ctx [:query-params :parent-type])
        parent-id (get-in ctx [:query-params :parent-id])
        property (get-in ctx [:query-params :property])
        body (:body-params ctx)
        resp (services/add-text-i18n parent-type parent-id property body)]
    (if (services/service-error? resp)
      (ajax/save-failed (meta resp))
      (ajax/success resp))))

(defn update-text
  [ctx]
  (let [parent-type (get-in ctx [:query-params :parent-type])
        parent-id (get-in ctx [:query-params :parent-id])
        property (get-in ctx [:query-params :property])
        text-i18n-id (get-in ctx [:query-params :text-i18n])
        body (:body-params ctx)
        resp (services/update-text-i18n parent-type parent-id property text-i18n-id body)]
    (if (services/service-error? resp)
      (ajax/save-failed (meta resp))
      (ajax/success resp))))
        
(defn delete-text
  [ctx]
  (let [parent-type (get-in ctx [:query-params :parent-type])
        parent-id (get-in ctx [:query-params :parent-id])
        property (get-in ctx [:query-params :property])
        text-i18n-id (get-in ctx [:query-params :text-i18n])
        resp (services/delete-text-i18n parent-type parent-id property text-i18n-id)]
    (if (services/service-error? resp)
      (ajax/save-failed (meta resp))
      (ajax/success resp))))

(def process-defns
  [{:name (str "put-api-text-i18n")
    :runnable-fn auth-on-parent-type
    :run-fn add-text
    :run-if-false ajax/forbidden}

   {:name (str "post-api-text-i18n")
    :runnable-fn auth-on-parent-type-text
    :run-fn update-text
    :run-if-false ajax/forbidden}

   {:name (str "delete-api-text-i18n")
    :runnable-fn auth-on-parent-type-text
    :run-fn delete-text
    :run-if-false ajax/forbidden}])

(process/register-processes (map #(DefaultProcess/create %) process-defns))
