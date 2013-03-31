(ns org.healthsciencessc.consent.services.process.user-role
  (:use     [pliant.process :only [defprocess as-method]])
  (:require [org.healthsciencessc.consent.services.data :as data]
            [org.healthsciencessc.consent.services.process.user :as user]
            [org.healthsciencessc.consent.services.respond :as respond]
            [org.healthsciencessc.consent.domain.types :as types]
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint]))

(defprocess add-userrole
  [ctx]
  (if (user/admins-user? ctx)
    (let [role-id (get-in ctx [:query-params :role])
          role (data/find-record types/role role-id)
          user-id (get-in ctx [:query-params :user])
          user (data/find-record types/user user-id)
          user-org (:organization user)
          loc-id (get-in ctx [:query-params :location])]
      (if (:requires-location role)
        (if loc-id
          (data/create types/role-mapping {:organization user-org
                                       :role {:id role-id}
                                       :user user
                                       :location {:id loc-id}})
          (let [loc-ids (map :id (data/find-children types/organization (:id user-org) types/location))]
            (if (empty? loc-ids)
              (data/create types/role-mapping {:organization user-org
                                           :role {:id role-id}
                                           :user user})
              (doall (map #(data/create types/role-mapping {:organization user-org
                                                        :role {:id role-id}
                                                        :user user
                                                        :location {:id %}})
                          loc-ids)))))
        (data/create types/role-mapping {:organization user-org
                                     :role {:id role-id}
                                     :user user}))
      (data/find-record types/user user-id))
    (respond/forbidden)))

(as-method add-userrole endpoint/endpoints "put-security-userrole")


(defprocess delete-userrole
  [ctx]
  (if (user/admins-user? ctx)
    (let [role-id (get-in ctx [:query-params :role])
          user-id (get-in ctx [:query-params :user])
          loc-id (get-in ctx [:query-params :location])]
      (if loc-id
        (let [role-mappings (data/find-children types/user user-id types/role-mapping)
              role-mapping (first (filter #(and (= role-id (get-in % [:role :id])) (= loc-id (get-in % [:location :id]))) role-mappings))]
          (data/delete types/role-mapping (:id role-mapping)))
        (let [role-mappings (filter #(= role-id (get-in % [:role :id])) (data/find-children types/user user-id types/role-mapping))]
          (doseq [{id :id} role-mappings]
            (data/delete types/role-mapping id))))
      (data/find-record types/user user-id))
    (respond/forbidden)))

(as-method delete-userrole endpoint/endpoints "delete-security-userrole")
