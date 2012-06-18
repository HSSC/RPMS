(ns org.healthsciencessc.rpms2.consent-services.default-processes.group-role
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils :only (admin? super-admin? some-kind-of-admin? forbidden-fn)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-domain.runnable :as runnable]
            [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-services.utils :as utils])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def group-role-processes
  [{:name "put-security-grouprole"
    :runnable-fn (runnable/gen-super-or-admin-record-check utils/current-user utils/get-group-record)
    :run-fn (fn [params]
              (let [role-id (get-in params [:query-params :role])
                    role (data/find-record "role" role-id)
                    group-id (get-in params [:query-params :group])
                    group (data/find-record "group" group-id)
                    group-org (:organization group)
                    loc-id (get-in params [:query-params :location])]
                (if (:requires-location role)
                  (if loc-id
                    (data/create "role-mapping" {:organization group-org
                                                 :role {:id role-id}
                                                 :group group
                                                 :location {:id loc-id}})
                    (let [loc-ids (map :id (data/find-children "organization" (:id group-org) "location"))]
                      (if (empty? loc-ids)
                        (data/create "role-mapping" {:organization group-org
                                                     :role {:id role-id}
                                                     :group group})
                        (doall (map #(data/create "role-mapping" {:organization group-org
                                                                  :role {:id role-id}
                                                                  :group group
                                                                  :location {:id %}})
                                    loc-ids)))))
                  (data/create "role-mapping" {:organization group-org
                                               :role {:id role-id}
                                               :group group}))
                (data/find-record "group" group-id)))
    :run-if-false forbidden-fn}
   
   {:name "delete-security-grouprole"
    :runnable-fn (fn [params]
                   (let [current-user (get-in params [:session :current-user])
                         current-user-org-id (get-in current-user [:organization :id])
                         role-id (get-in params [:query-params :role])
                         group-id (get-in params [:query-params :group])
                         loc-id (get-in params [:query-params :location])
                         org-id (get-in params [:query-params :organization])]
                     (or (and (super-admin? current-user) org-id role-id group-id)
                         (and (admin? current-user)
                              (and role-id (data/belongs-to? "role" role-id "organization" current-user-org-id))
                              (and group-id (data/belongs-to? "group" group-id "organization" current-user-org-id))
                              (if org-id (= org-id current-user-org-id) true)
                              (if loc-id (data/belongs-to? "location" loc-id "organization" current-user-org-id) true)))))
    :run-fn (fn [params]
              (let [current-user (get-in params [:session :current-user])
                    current-user-org-id (get-in current-user [:organization :id])
                    role-id (get-in params [:query-params :role])
                    group-id (get-in params [:query-params :group])
                    loc-id (get-in params [:query-params :location])
                    q-org-id (get-in params [:query-params :organization])
                    org-id (or q-org-id current-user-org-id)]
                (if loc-id
                  (let [role-mappings (data/find-children "group" group-id "role-mapping")
                        role-mapping (first (filter #(and (= role-id (get-in % [:role :id])) (= loc-id (get-in % [:location :id]))) role-mappings))]
                    (data/delete "role-mapping" (:id role-mapping)))
                  (let [role-mappings (filter #(= role-id (get-in % [:role :id])) (data/find-children "group" group-id "role-mapping"))]
                    (doseq [{id :id} role-mappings]
                      (data/delete "role-mapping" id))))))
    :run-if-false forbidden-fn}])

(process/register-processes (map #(DefaultProcess/create %) group-role-processes))