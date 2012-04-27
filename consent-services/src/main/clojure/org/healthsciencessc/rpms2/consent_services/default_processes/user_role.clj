(ns org.healthsciencessc.rpms2.consent-services.default-processes.user-role
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils :only (admin? super-admin? some-kind-of-admin? forbidden-fn)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def user-role-processes
  [{:name "put-security-userrole"
    :runnable-fn (fn [params]
                   (let [current-user (get-in params [:session :current-user])
                         current-user-org-id (get-in current-user [:organization :id])
                         role-id (get-in params [:query-params :role])
                         user-id (get-in params [:query-params :user])
                         loc-id (get-in params [:query-params :location])
                         org-id (get-in params [:query-params :organization])]
                     (or (and (super-admin? current-user) org-id role-id user-id)
                         (and (admin? current-user)
                              (and role-id (data/belongs-to? "role" role-id "organization" current-user-org-id))
                              (and user-id (data/belongs-to? "user" user-id "organization" current-user-org-id))
                              (if org-id (= org-id current-user-org-id) true)
                              (if loc-id (data/belongs-to? "location" loc-id "organization" current-user-org-id) true)))))
    :run-fn (fn [params]
              (let [current-user (get-in params [:session :current-user])
                    current-user-org-id (get-in current-user [:organization :id])
                    role-id (get-in params [:query-params :role])
                    user-id (get-in params [:query-params :user])
                    loc-id (get-in params [:query-params :location])
                    q-org-id (get-in params [:query-params :organization])
                    org-id (or q-org-id current-user-org-id)]
                (if loc-id
                  (data/create "role-mapping" {:organization {:id org-id}
                                               :role {:id role-id}
                                               :user {:id user-id}
                                               :location {:id loc-id}})
                  (let [loc-ids (map :id (data/find-children "organization" org-id "location"))]
                    (map #(data/create "role-mapping" {:organization {:id org-id}
                                                       :role {:id role-id}
                                                       :user {:id user-id}
                                                       :location {:id %}})
                         loc-ids)))
                (data/find-record "user" user-id)))
    :run-if-false forbidden-fn}
   
   {:name "delete-security-userrole"
    :runnable-fn (fn [params]
                   (let [current-user (get-in params [:session :current-user])
                         current-user-org-id (get-in current-user [:organization :id])
                         role-id (get-in params [:query-params :role])
                         user-id (get-in params [:query-params :user])
                         loc-id (get-in params [:query-params :location])
                         org-id (get-in params [:query-params :organization])]
                     (or (and (super-admin? current-user) org-id role-id user-id)
                         (and (admin? current-user)
                              (and role-id (data/belongs-to? "role" role-id "organization" current-user-org-id))
                              (and user-id (data/belongs-to? "user" user-id "organization" current-user-org-id))
                              (if org-id (= org-id current-user-org-id) true)
                              (if loc-id (data/belongs-to? "location" loc-id "organization" current-user-org-id) true)))))
    :run-fn (fn [params]
              (let [current-user (get-in params [:session :current-user])
                    current-user-org-id (get-in current-user [:organization :id])
                    role-id (get-in params [:query-params :role])
                    user-id (get-in params [:query-params :user])
                    loc-id (get-in params [:query-params :location])
                    q-org-id (get-in params [:query-params :organization])
                    org-id (or q-org-id current-user-org-id)]
                (if loc-id
                  (let [role-mappings (data/find-children "user" user-id "role-mapping")
                        role-mapping (filter #(and (= role-id (get-in % [:role :id])) (= loc-id (get-in % [:location :id]))) role-mappings)]
                    (data/delete "role-mapping" (:id role-mapping)))
                  (let [role-mappings (filter #(= role-id (get-in % [:role-id])) (data/find-children "user" user-id "role-mapping"))]
                    (doseq [{id :id} role-mappings]
                      (data/delete "role-mapping" id))))))
    :run-if-false forbidden-fn}])

(process/register-processes (map #(DefaultProcess/create %) user-role-processes))