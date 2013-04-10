(ns org.healthsciencessc.consent.services.process.location
  (:use     [pliant.process :only [defprocess as-method]])
  (:require [org.healthsciencessc.consent.services.data :as data]
            [org.healthsciencessc.consent.services.respond :as respond]
            [org.healthsciencessc.consent.services.session :as session]
            [org.healthsciencessc.consent.services.vouch :as vouch]
            [org.healthsciencessc.consent.common.roles :as roles]
            [org.healthsciencessc.consent.common.types :as types]
            [pliant.webpoint.request :as endpoint]))


(defprocess get-locations
  [ctx]
  (let [user (session/current-user ctx)
        org-id (get-in ctx [:query-params :organization])]
    (cond
      (roles/superadmin? user)
        (data/find-children types/organization (or org-id (session/current-org-id ctx)) types/group)
      (roles/admin? user)
        (data/find-children types/organization (session/current-org-id ctx) types/location)
      :else
        (respond/forbidden))))

(as-method get-locations endpoint/endpoints "get-security-locations")

(defprocess get-location
  [ctx]
  (if (vouch/admins-location? ctx)
    (let [location-id (get-in ctx [:query-params :location])]
      (data/find-record types/location location-id))
    (respond/forbidden)))

(as-method get-location endpoint/endpoints "get-security-location")


(defprocess add-location
  [ctx]
  (if (vouch/admins-org? ctx)
    (let [data (:body-params ctx)
          org-id (get-in ctx [:query-params :organization])]
      (data/create types/location (assoc data :organization {:id org-id})))
    (respond/forbidden)))

(as-method add-location endpoint/endpoints "put-security-location")


(defprocess update-location
  [ctx]
  (if (vouch/admins-location? ctx)
    (let [loc-id (get-in ctx [:query-params :location])
          loc-data (:body-params ctx)]
      (data/update types/location loc-id loc-data))
    (respond/forbidden)))

(as-method update-location endpoint/endpoints "post-security-location")


(defprocess delete-location
  [ctx]
  (if (vouch/admins-location? ctx)
    (let [loc-id (get-in ctx [:query-params :location])]
      (data/delete types/location loc-id))
    (respond/forbidden)))

(as-method delete-location endpoint/endpoints "delete-security-location")
