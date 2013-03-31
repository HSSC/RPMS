(ns org.healthsciencessc.consent.services.process.policy-definition
  (:use     [pliant.process :only [defprocess as-method]])
  (:require [org.healthsciencessc.consent.services.data :as data]
            [org.healthsciencessc.consent.services.respond :as respond]
            [org.healthsciencessc.consent.services.session :as session]
            [org.healthsciencessc.consent.services.vouch :as vouch]
            [org.healthsciencessc.consent.domain.roles :as roles]
            [org.healthsciencessc.consent.domain.types :as types]
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint]))


(defn designs-policy-definition
  [ctx]
  (vouch/designs-type ctx types/policy-definition (get-in ctx [:query-params :policy-definition])))

(defn views-policy-definition
  [ctx]
  (vouch/views-type-as-designer ctx types/policy-definition (get-in ctx [:query-params :policy-definition])))


(defprocess get-policy-definitions
  [ctx]
  (let [user (session/current-user ctx)]
    (if (roles/protocol-designer? user)
      (data/find-children types/organization (session/current-org-id ctx) types/policy-definition)
      (respond/forbidden))))

(as-method get-policy-definitions endpoint/endpoints "get-library-policy-definitions")


(defprocess get-policy-definition
  [ctx]
  (let [policy-definition (views-policy-definition ctx)]
    (if policy-definition
      policy-definition
      (respond/forbidden))))

(as-method get-policy-definition endpoint/endpoints "get-library-policy-definition")


(defprocess add-policy-definition
  [ctx]
  (if (vouch/designs-org? ctx)
    (let [org-id (get-in ctx [:query-params :organization])
          data (assoc (:body-params ctx) :organization {:id org-id})]
      (data/create types/policy-definition data))
    (respond/forbidden)))

(as-method add-policy-definition endpoint/endpoints "put-library-policy-definition")


(defprocess update-policy-definition
  [ctx]
  (let [policy-definition (designs-policy-definition ctx)]
    (if policy-definition
      (data/update types/policy-definition (:id policy-definition) (:body-params ctx))
      (respond/forbidden))))

(as-method update-policy-definition endpoint/endpoints "post-library-policy-definition")


(defprocess delete-policy-definition
  [ctx]
  (let [policy-definition (designs-policy-definition ctx)]
    (if policy-definition
      (data/delete types/policy-definition (:id policy-definition))
      (respond/forbidden))))

(as-method delete-policy-definition endpoint/endpoints "delete-library-policy-definition")
