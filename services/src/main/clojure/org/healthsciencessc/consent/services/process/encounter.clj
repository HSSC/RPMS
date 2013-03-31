(ns org.healthsciencessc.consent.services.process.encounter
  (:use     [pliant.process :only [defprocess as-method]])
  (:require [org.healthsciencessc.consent.services.data :as data]
            [org.healthsciencessc.consent.services.respond :as respond]
            [org.healthsciencessc.consent.services.vouch :as vouch]
            [org.healthsciencessc.consent.domain.types :as types]
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint]))


(defprocess get-encounters
  [ctx]
  (let [location-id (get-in ctx [:query-params :location])
        type (if location-id types/location types/organization)
        record (if location-id (vouch/collects-or-manages-location ctx) (vouch/collects-or-manages ctx))]
    (if record
      (data/find-children type (:id record) types/encounter)
      (respond/forbidden))))

(as-method get-encounters endpoint/endpoints "get-consent-encounters")


(defprocess get-encounter
  [ctx]
  (let [encounter (vouch/collects-or-manages-encounter ctx)]
    (if encounter
      encounter
      (respond/forbidden))))

(as-method get-encounter endpoint/endpoints "get-consent-encounter")


(defprocess get-encounter-consents
  [ctx]
  (let [encounter (vouch/collects-or-manages-encounter ctx)]
    (if encounter
      (data/find-children types/encounter (:id encounter) types/consent)
      (respond/forbidden))))

(as-method get-encounter-consents endpoint/endpoints "get-consent-encounter-consents")


(defprocess get-encounter-endorsements
  [ctx]
  (let [encounter (vouch/collects-or-manages-encounter ctx)]
    (if encounter
      (data/find-children types/encounter (:id encounter) types/consent-endorsement)
      (respond/forbidden))))

(as-method get-encounter-endorsements endpoint/endpoints "get-consent-encounter-consent-endorsements")


(defprocess get-encounter-meta-items
  [ctx]
  (let [encounter (vouch/collects-or-manages-encounter ctx)]
    (if encounter
      (data/find-children types/encounter (:id encounter) types/consent-meta-item)
      (respond/forbidden))))

(as-method get-encounter-meta-items endpoint/endpoints "get-consent-encounter-consent-meta-items")

(defprocess add-encounter
  [ctx]
  (let [location (vouch/collects-location ctx)]
    (if location
      (let [org (:organization location)
            data (assoc (:body-params ctx) :organization org :location location)]
        (data/create types/encounter data))
      (respond/forbidden))))

(as-method add-encounter endpoint/endpoints "put-consent-encounter")


(defprocess update-encounter
  [ctx]
  (let [encounter (vouch/collects-encounter ctx)]
    (if encounter
      (data/update types/encounter (:id encounter) (:body-params ctx))
      (respond/forbidden))))

(as-method update-encounter endpoint/endpoints "post-consent-encounter")


(defprocess delete-encounter
  [ctx]
  (let [encounter (vouch/collects-encounter ctx)]
    (if encounter
      (data/delete types/encounter (:id encounter))
      (respond/forbidden))))

(as-method delete-encounter endpoint/endpoints "delete-consent-encounter")

