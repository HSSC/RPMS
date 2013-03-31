(ns org.healthsciencessc.consent.services.process.policy
  (:use     [pliant.process :only [defprocess as-method]])
  (:require [org.healthsciencessc.consent.services.data :as data]
            [org.healthsciencessc.consent.services.respond :as respond]
            [org.healthsciencessc.consent.services.session :as session]
            [org.healthsciencessc.consent.services.vouch :as vouch]
            [org.healthsciencessc.consent.domain.roles :as roles]
            [org.healthsciencessc.consent.domain.types :as types]
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint]))

(defn designs-policy
  [ctx]
  (vouch/designs-type ctx types/policy (get-in ctx [:query-params :policy])))

(defn views-policy
  [ctx]
  (vouch/views-type-as-designer ctx types/policy (get-in ctx [:query-params :policy])))


(defprocess get-policies
  [ctx]
  (let [user (session/current-user ctx)]
    (if (roles/protocol-designer? user)
      (data/find-children types/organization (session/current-org-id ctx) types/policy)
      (respond/forbidden))))

(as-method get-policies endpoint/endpoints "get-library-policies")


(defprocess get-policy
  [ctx]
  (let [policy (views-policy ctx)]
    (if policy
      policy
      (respond/forbidden))))

(as-method get-policy endpoint/endpoints "get-library-policy")


(defprocess add-policy
  [ctx]
  (if (vouch/designs-org? ctx)
    (let [org-id (get-in ctx [:query-params :organization])
          data (assoc (:body-params ctx) :organization {:id org-id})]
      (data/create types/policy data))
    (respond/forbidden)))

(as-method add-policy endpoint/endpoints "put-library-policy")


(defprocess update-policy
  [ctx]
  (let [policy (designs-policy ctx)]
    (if policy
      (data/update types/policy (:id policy) (:body-params ctx))
      (respond/forbidden))))

(as-method update-policy endpoint/endpoints "post-library-policy")


(defprocess delete-policy
  [ctx]
  (let [policy (designs-policy ctx)]
    (if policy
      (data/delete types/policy (:id policy))
      (respond/forbidden))))

(as-method delete-policy endpoint/endpoints "delete-library-policy")
