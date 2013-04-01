(ns org.healthsciencessc.consent.services.process.endorsement
  (:use     [pliant.process :only [defprocess as-method]])
  (:require [org.healthsciencessc.consent.services.data :as data]
            [org.healthsciencessc.consent.services.respond :as respond]
            [org.healthsciencessc.consent.services.session :as session]
            [org.healthsciencessc.consent.services.vouch :as vouch]
            [org.healthsciencessc.consent.domain.roles :as roles]
            [org.healthsciencessc.consent.domain.types :as types]
            [pliant.webpoint.request :as endpoint]))

(defn designs-endorsement
  [ctx]
  (vouch/designs-type ctx types/endorsement (get-in ctx [:query-params :endorsement])))

(defn views-endorsement
  [ctx]
  (vouch/views-type-as-designer ctx types/endorsement (get-in ctx [:query-params :endorsement])))

(defprocess get-endorsements
  [ctx]
  (let [user (session/current-user ctx)]
    (if (roles/protocol-designer? user)
      (data/find-children types/organization (session/current-org-id ctx) types/endorsement)
      (respond/forbidden))))

(as-method get-endorsements endpoint/endpoints "get-library-endorsements")


(defprocess get-endorsement
  [ctx]
  (let [endorsement (views-endorsement ctx)]
    (if endorsement
      endorsement
      (respond/forbidden))))

(as-method get-endorsement endpoint/endpoints "get-library-endorsement")


(defprocess add-endorsement
  [ctx]
  (if (vouch/designs-org? ctx)
    (let [org-id (get-in ctx [:query-params :organization])
          data (assoc (:body-params ctx) :organization {:id org-id})]
      (data/create types/endorsement data))
    (respond/forbidden)))

(as-method add-endorsement endpoint/endpoints "put-library-endorsement")


(defprocess update-endorsement
  [ctx]
  (let [endorsement (designs-endorsement ctx)]
    (if endorsement
      (data/update types/endorsement (:id endorsement) (:body-params ctx))
      (respond/forbidden))))

(as-method update-endorsement endpoint/endpoints "post-library-endorsement")


(defprocess delete-endorsement
  [ctx]
  (let [endorsement (designs-endorsement ctx)]
    (if endorsement
      (data/delete types/endorsement (:id endorsement))
      (respond/forbidden))))

(as-method delete-endorsement endpoint/endpoints "delete-library-endorsement")


(defprocess assign-endorsement-type
  [ctx]
  (let [endorsement (designs-endorsement ctx)]
    (if endorsement
      (let [current-id (get-in endorsement [:endorsement-type :id])
            assign-id (get-in ctx [:query-params :assign-type])]
        (if (not= current-id assign-id)
          (data/re-relate-records types/endorsement (:id endorsement) types/endorsement-type current-id assign-id)))
      (respond/forbidden))))

(as-method assign-endorsement-type endpoint/endpoints "post-library-endorsement-endorsement-type")
