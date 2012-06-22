;; Provides common process patterns.
(ns org.healthsciencessc.rpms2.consent-admin.process.common
  (:require [org.healthsciencessc.rpms2.consent-admin.ajax :as ajax]
            [org.healthsciencessc.rpms2.consent-admin.security :as security]
            [org.healthsciencessc.rpms2.consent-admin.services :as services]
            [org.healthsciencessc.rpms2.consent-domain.lookup :as lookup]))

(def lookup-organization (lookup/gen-organization-lookup-in-query security/current-org-id))

(defn replace-truths
  "Changes the values in a map to be either true or false depending if the value has been deemed
   to be a truth."
  [m & truths]
  (let [serum (set truths)]
    (into {} (for [[k v] m] [k (if (serum v) true false)]))))

(defn find-and-replace-truths
  "Replaces specific values in a map that are flagged for thruthfulness as either true or false."
  [m props & truths]
  (merge m (apply replace-truths (select-keys m props) truths)))

(defn owned-by-user-org
  [record]
  (let [record-org-id (get-in record [:organization :id])
        user-org-id (security/current-org-id {})]
    (= record-org-id user-org-id)))

;; Provide Process Generation Functions
(defn gen-api-type-delete
  "Generates a function that implements a common pattern for deleting a type using an ajax request."
  [servicefn lookupfn missing-id-message]
  (fn [ctx]
    (if-let [node-id (lookupfn ctx)]
      (let [resp (servicefn node-id)]
        ;; Handle Error or Success
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      ;; Handle Bad Request
      (ajax/error {:message missing-id-message}))))

(defn get-api-type-add
  "Adds a new protocol to a location."
  [servicefn]
  (fn [ctx]
    (let [org-id (lookup-organization ctx)
          body (assoc (:body-params ctx) :organization {:id org-id})
          resp (servicefn body)]
      (if (services/service-error? resp)
        (ajax/save-failed (meta resp))
        (ajax/success resp)))))

(defn gen-api-type-update
  "Updates a protocol."
  [servicefn lookupfn missing-id-message]
  (fn [ctx]
    (if-let [node-id (lookupfn ctx)]
      (let [body (:body-params ctx)
            resp (servicefn node-id body)]
        ;; Handle Error or Success
        (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp)))
      ;; Handle Bad Request
      (ajax/error {:message missing-id-message}))))

