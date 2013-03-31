(ns org.healthsciencessc.consent.services.process.role
  (:use     [pliant.process :only [defprocess as-method]])
  (:require [org.healthsciencessc.consent.services.data :as data]
            [org.healthsciencessc.consent.services.respond :as respond]
            [org.healthsciencessc.consent.services.session :as session]
            [org.healthsciencessc.consent.services.vouch :as vouch]
            [org.healthsciencessc.consent.domain.roles :as roles]
            [org.healthsciencessc.consent.domain.tenancy :as tenancy]
            [org.healthsciencessc.consent.domain.types :as types]
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint]))

(defn admins-role?
  [ctx]
  (let [user (session/current-user ctx)
        user-org-id (get-in user [:organization :id])
        role-id (get-in ctx [:query-params :role])]
    (or (roles/superadmin? user)
        (and (roles/admin? user) 
             (data/belongs-to? types/role role-id types/organization user-org-id false)))))

(defprocess get-roles
  [ctx]
  (let [user (session/current-user ctx)
        org-id (get-in ctx [:query-params :organization])]
    (cond
      (roles/superadmin? user)
        (data/find-children types/organization (or org-id (session/current-org-id ctx)) types/group)
      (roles/admin? user)
        (data/find-children types/organization (session/current-org-id ctx) types/role)
      :else
        (respond/forbidden))))

(as-method get-roles endpoint/endpoints "get-security-roles")

(defprocess get-role
  [ctx]
  (let [role-id (get-in ctx [:query-params :role])
        role (data/find-record types/role role-id)]
    (if (or (admins-role? ctx) (tenancy/belongs-to-base? role))
      role
      (respond/forbidden))))

(as-method get-role endpoint/endpoints "get-security-role")


(defprocess add-role
  [ctx]
  (if (vouch/admins-org? ctx)
    (let [data (:body-params ctx)
          org-id (get-in ctx [:query-params :organization])]
      (data/create types/role (assoc data :organization {:id org-id})))
    (respond/forbidden)))

(as-method add-role endpoint/endpoints "put-security-role")


(defprocess update-role
  [ctx]
  (if (admins-role? ctx)
    (let [role-id (get-in ctx [:query-params :role])
          role-data (:body-params ctx)]
      (data/update types/role role-id role-data))
    (respond/forbidden)))

(as-method update-role endpoint/endpoints "post-security-role")


(defprocess delete-role
  [ctx]
  (if (admins-role? ctx)
    (let [role-id (get-in ctx [:query-params :role])]
      (data/delete types/role role-id))
    (respond/forbidden)))

(as-method delete-role endpoint/endpoints "delete-security-role")

