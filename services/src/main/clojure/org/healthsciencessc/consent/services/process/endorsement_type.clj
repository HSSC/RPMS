(ns org.healthsciencessc.consent.services.process.endorsement-type
  (:use     [pliant.process :only [defprocess as-method]])
  (:require [org.healthsciencessc.consent.services.data :as data]
            [org.healthsciencessc.consent.services.respond :as respond]
            [org.healthsciencessc.consent.services.session :as session]
            [org.healthsciencessc.consent.services.vouch :as vouch]
            [org.healthsciencessc.consent.common.roles :as roles]
            [org.healthsciencessc.consent.common.types :as types]
            [pliant.webpoint.request :as endpoint]))


(defn designs-endorsement-type
  [ctx]
  (vouch/designs-type ctx types/endorsement-type (get-in ctx [:query-params :endorsement-type])))

(defn views-endorsement-type
  [ctx]
  (vouch/views-type-as-designer ctx types/endorsement-type (get-in ctx [:query-params :endorsement-type])))

(defprocess get-endorsement-types
  [ctx]
  (let [user (session/current-user ctx)]
    (if (roles/protocol-designer? user)
      (data/find-children types/organization (session/current-org-id ctx) types/endorsement-type)
      (respond/forbidden))))

(as-method get-endorsement-types endpoint/endpoints "get-library-endorsement-types")


(defprocess get-endorsement-type
  [ctx]
  (let [endorsement-type (views-endorsement-type ctx)]
    (if endorsement-type
      endorsement-type
      (respond/forbidden))))

(as-method get-endorsement-type endpoint/endpoints "get-library-endorsement-type")


(defprocess add-endorsement-type
  [ctx]
  (if (vouch/designs-org? ctx)
    (let [org-id (get-in ctx [:query-params :organization])
          data (assoc (:body-params ctx) :organization {:id org-id})]
      (data/create types/endorsement-type data))
    (respond/forbidden)))

(as-method add-endorsement-type endpoint/endpoints "put-library-endorsement-type")


(defprocess update-endorsement-type
  [ctx]
  (let [endorsement-type (designs-endorsement-type ctx)]
    (if endorsement-type
      (data/update types/endorsement-type (:id endorsement-type) (:body-params ctx))
      (respond/forbidden))))

(as-method update-endorsement-type endpoint/endpoints "post-library-endorsement-type")


(defprocess delete-endorsement-type
  [ctx]
  (let [endorsement-type (designs-endorsement-type ctx)]
    (if endorsement-type
      (data/delete types/endorsement-type (:id endorsement-type))
      (respond/forbidden))))

(as-method delete-endorsement-type endpoint/endpoints "delete-library-endorsement-type")
