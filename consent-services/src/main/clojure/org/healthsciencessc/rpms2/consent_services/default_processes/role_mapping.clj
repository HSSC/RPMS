(ns org.healthsciencessc.rpms2.consent-services.default-processes.role-mapping
  (:use     [pliant.process :only [defprocess as-method]])
  (:require [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-services.respond :as respond]
            [org.healthsciencessc.rpms2.consent-services.session :as session]
            [org.healthsciencessc.rpms2.consent-domain.roles :as roles]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint]))

(defn admins-role-mapping?
  [ctx]
  (let [user (session/current-user ctx)
        user-org-id (get-in user [:organization :id])
        role-mapping-id (get-in ctx [:query-params :role-mapping])]
    (or (roles/superadmin? user)
        (and (roles/admin? user) 
             (data/belongs-to? types/role-mapping role-mapping-id types/organization user-org-id false)))))

(defprocess delete-role-mapping
  [ctx]
  (if (admins-role-mapping? ctx)
    (let [role-mapping-id (get-in ctx [:query-params :role-mapping])]
      (data/delete types/role-mapping role-mapping-id))
    (respond/forbidden)))

(as-method delete-role-mapping endpoint/endpoints "delete-security-role-mapping")

