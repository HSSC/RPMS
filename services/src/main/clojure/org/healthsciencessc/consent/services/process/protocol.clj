(ns org.healthsciencessc.consent.services.process.protocol
  (:use     [pliant.process :only [defprocess as-method]])
  (:require [org.healthsciencessc.consent.services.data :as data]
            [org.healthsciencessc.consent.services.respond :as respond]
            [org.healthsciencessc.consent.services.vouch :as vouch]
            [org.healthsciencessc.consent.domain.roles :as roles]
            [org.healthsciencessc.consent.domain.types :as types]
            [pliant.webpoint.request :as endpoint]))

(defprocess get-protocols
  [ctx]
  (let [location (vouch/designs-location ctx)]
    (if location
      (sort-by :name (data/find-children types/location (:id location) types/protocol))
      (respond/forbidden))))

(as-method get-protocols endpoint/endpoints "get-protocols")


(defprocess get-protocol
  [ctx]
  (let [protocol (vouch/designs-protocol ctx)]
    (if protocol
      protocol
      (respond/forbidden))))

(as-method get-protocol endpoint/endpoints "get-protocol")


(defprocess add-protocol
  [ctx]
  (let [location (vouch/designs-location ctx)]
    (if location
      (let [org (:organization location)
            data (assoc (:body-params ctx) :organization org :location location)]
        (data/create types/protocol data))
      (respond/forbidden))))

(as-method add-protocol endpoint/endpoints "put-protocol")


(defprocess update-protocol
  [ctx]
  (let [protocol (vouch/designs-protocol ctx)]
    (if protocol
      (data/update types/protocol (:id protocol) (:body-params ctx))
      (respond/forbidden))))

(as-method update-protocol endpoint/endpoints "post-protocol")


(defprocess delete-protocol
  [ctx]
  (let [protocol (vouch/designs-protocol ctx)]
    (if protocol
      (data/delete types/protocol (:id protocol))
      (respond/forbidden))))

(as-method delete-protocol endpoint/endpoints "delete-protocol")
