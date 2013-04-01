(ns org.healthsciencessc.consent.services.process.group
  (:use     [pliant.process :only [defprocess as-method]])
  (:require [org.healthsciencessc.consent.services.data :as data]
            [org.healthsciencessc.consent.services.respond :as respond]
            [org.healthsciencessc.consent.services.session :as session]
            [org.healthsciencessc.consent.services.vouch :as vouch]
            [org.healthsciencessc.consent.domain.roles :as roles]
            [org.healthsciencessc.consent.domain.types :as types]
            [pliant.webpoint.request :as endpoint]))

(defn admins-group?
  [ctx]
  (let [user (session/current-user ctx)
        user-org-id (get-in user [:organization :id])
        group-id (get-in ctx [:query-params :group])]
    (or (roles/superadmin? user)
        (and (roles/admin? user) 
             (data/belongs-to? types/group group-id types/organization user-org-id false)))))

(defprocess get-groups
  [ctx]
  (let [user (session/current-user ctx)
        org-id (get-in ctx [:query-params :organization])]
    (cond
      (roles/superadmin? user)
        (data/find-children types/organization (or org-id (session/current-org-id ctx)) types/group)
      (roles/admin? user)
        (data/find-children types/organization (session/current-org-id ctx) types/group)
      :else
        (respond/forbidden))))

(as-method get-groups endpoint/endpoints "get-security-groups")

(defprocess get-group
  [ctx]
  (if (admins-group? ctx)
    (let [group-id (get-in ctx [:query-params :group])]
      (data/find-record types/group group-id))
    (respond/forbidden)))

(as-method get-group endpoint/endpoints "get-security-group")


(defprocess add-group
  [ctx]
  (if (vouch/admins-org? ctx)
    (let [data (:body-params ctx)
          org-id (get-in ctx [:query-params :organization])]
      (data/create types/group (assoc data :organization {:id org-id})))
    (respond/forbidden)))

(as-method add-group endpoint/endpoints "put-security-group")


(defprocess update-group
  [ctx]
  (if (admins-group? ctx)
    (let [group-id (get-in ctx [:query-params :group])
          group-data (:body-params ctx)]
      (data/update types/group group-id group-data))
    (respond/forbidden)))

(as-method update-group endpoint/endpoints "post-security-group")


(defprocess delete-group
  [ctx]
  (if (admins-group? ctx)
    (let [group-id (get-in ctx [:query-params :group])]
      (data/delete types/group group-id))
    (respond/forbidden)))

(as-method delete-group endpoint/endpoints "delete-security-group")

