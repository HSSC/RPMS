(ns org.healthsciencessc.consent.services.process.meta-item
  (:use     [pliant.process :only [defprocess as-method]])
  (:require [org.healthsciencessc.consent.services.data :as data]
            [org.healthsciencessc.consent.services.respond :as respond]
            [org.healthsciencessc.consent.services.session :as session]
            [org.healthsciencessc.consent.services.vouch :as vouch]
            [org.healthsciencessc.consent.common.roles :as roles]
            [org.healthsciencessc.consent.common.types :as types]
            [pliant.webpoint.request :as endpoint]))


(defn designs-meta-item
  [ctx]
  (vouch/designs-type ctx types/meta-item (get-in ctx [:query-params :meta-item])))

(defn views-meta-item
  [ctx]
  (vouch/views-type-as-designer ctx types/meta-item (get-in ctx [:query-params :meta-item])))


(defprocess get-meta-items
  [ctx]
  (let [user (session/current-user ctx)]
    (if (roles/protocol-designer? user)
      (data/find-children types/organization (session/current-org-id ctx) types/meta-item)
      (respond/forbidden))))

(as-method get-meta-items endpoint/endpoints "get-library-meta-items")


(defprocess get-meta-item
  [ctx]
  (let [meta-item (views-meta-item ctx)]
    (if meta-item
      meta-item
      (respond/forbidden))))

(as-method get-meta-item endpoint/endpoints "get-library-meta-item")


(defprocess add-meta-item
  [ctx]
  (if (vouch/designs-org? ctx)
    (let [org-id (get-in ctx [:query-params :organization])
          data (assoc (:body-params ctx) :organization {:id org-id})]
      (data/create types/meta-item data))
    (respond/forbidden)))

(as-method add-meta-item endpoint/endpoints "put-library-meta-item")


(defprocess update-meta-item
  [ctx]
  (let [meta-item (designs-meta-item ctx)]
    (if meta-item
      (data/update types/meta-item (:id meta-item) (:body-params ctx))
      (respond/forbidden))))

(as-method update-meta-item endpoint/endpoints "post-library-meta-item")


(defprocess delete-meta-item
  [ctx]
  (let [meta-item (designs-meta-item ctx)]
    (if meta-item
      (data/delete types/meta-item (:id meta-item))
      (respond/forbidden))))

(as-method delete-meta-item endpoint/endpoints "delete-library-meta-item")
