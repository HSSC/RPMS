(ns org.healthsciencessc.consent.services.process.group-role
  (:use     [pliant.process :only [defprocess as-method]])
  (:require [org.healthsciencessc.consent.services.data :as data]
            [org.healthsciencessc.consent.services.process.group :as group]
            [org.healthsciencessc.consent.services.respond :as respond]
            [org.healthsciencessc.consent.domain.types :as types]
            [pliant.webpoint.request :as endpoint]))

(defprocess add-grouprole
  [ctx]
  (if (group/admins-group? ctx)
    (let [role-id (get-in ctx [:query-params :role])
          role (data/find-record types/role role-id)
          group-id (get-in ctx [:query-params :group])
          group (data/find-record types/group group-id)
          group-org (:organization group)
          loc-id (get-in ctx [:query-params :location])]
      (if (:requires-location role)
        (if loc-id
          (data/create types/role-mapping {:organization group-org
                                       :role {:id role-id}
                                       :group group
                                       :location {:id loc-id}})
          (let [loc-ids (map :id (data/find-children types/organization (:id group-org) types/location))]
            (if (empty? loc-ids)
              (data/create types/role-mapping {:organization group-org
                                           :role {:id role-id}
                                           :group group})
              (doall (map #(data/create types/role-mapping {:organization group-org
                                                        :role {:id role-id}
                                                        :group group
                                                        :location {:id %}})
                          loc-ids)))))
        (data/create types/role-mapping {:organization group-org
                                     :role {:id role-id}
                                     :group group}))
      (data/find-record types/group group-id))
    (respond/forbidden)))

(as-method add-grouprole endpoint/endpoints "put-security-grouprole")


(defprocess delete-grouprole
  [ctx]
  (if (group/admins-group? ctx)
    (let [role-id (get-in ctx [:query-params :role])
          group-id (get-in ctx [:query-params :group])
          loc-id (get-in ctx [:query-params :location])]
      (if loc-id
        (let [role-mappings (data/find-children types/group group-id types/role-mapping)
              role-mapping (first (filter #(and (= role-id (get-in % [:role :id])) (= loc-id (get-in % [:location :id]))) role-mappings))]
          (data/delete types/role-mapping (:id role-mapping)))
        (let [role-mappings (filter #(= role-id (get-in % [:role :id])) (data/find-children types/group group-id types/role-mapping))]
          (doseq [{id :id} role-mappings]
            (data/delete types/role-mapping id))))
      (data/find-record types/group group-id))
    (respond/forbidden)))

(as-method delete-grouprole endpoint/endpoints "delete-security-grouprole")