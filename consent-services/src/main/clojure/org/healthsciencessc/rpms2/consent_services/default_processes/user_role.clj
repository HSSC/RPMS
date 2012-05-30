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
                         loc-id (get-in params [:query-params :location])]
                     (or (and (super-admin? current-user) role-id user-id)
                         (and (admin? current-user)
                              (and role-id (data/belongs-to? "role" role-id "organization" current-user-org-id))
                              (and user-id (data/belongs-to? "user" user-id "organization" current-user-org-id))
                              (if loc-id (data/belongs-to? "location" loc-id "organization" current-user-org-id) true)))))
    :run-fn (fn [params]
              (let [role-id (get-in params [:query-params :role])
                    user-id (get-in params [:query-params :user])
                    user (data/find-record "user" user-id)
                    user-org (:organization user)
                    loc-id (get-in params [:query-params :location])]
                (if loc-id
                  (data/create "role-mapping" {:organization user-org
                                               :role {:id role-id}
                                               :user user
                                               :location {:id loc-id}})
                  (let [loc-ids (map :id (data/find-children "organization" (:id user-org) "location"))]
                    (if (empty? loc-ids)
                      (data/create "role-mapping" {:organization user-org
                                                   :role {:id role-id}
                                                   :user user})
                      (doall (map #(data/create "role-mapping" {:organization user-org
                                                                :role {:id role-id}
                                                                :user user
                                                                :location {:id %}})
                                  loc-ids)))))
                (data/find-record "user" user-id)))
    :run-if-false forbidden-fn}

   {:name "delete-security-userrole"
    :runnable-fn (fn [params]
                   (let [current-user (get-in params [:session :current-user])
                         current-user-org-id (get-in current-user [:organization :id])
                         role-id (get-in params [:query-params :role])
                         user-id (get-in params [:query-params :user])
                         loc-id (get-in params [:query-params :location])]
                     (or (and (super-admin? current-user) role-id user-id)
                         (and (admin? current-user)
                              (and role-id (data/belongs-to? "role" role-id "organization" current-user-org-id))
                              (and user-id (data/belongs-to? "user" user-id "organization" current-user-org-id))
                              (if loc-id (data/belongs-to? "location" loc-id "organization" current-user-org-id) true)))))
    :run-fn (fn [params]
              (let [role-id (get-in params [:query-params :role])
                    user-id (get-in params [:query-params :user])
                    loc-id (get-in params [:query-params :location])]
                (if loc-id
                  (let [role-mappings (data/find-children "user" user-id "role-mapping")
                        role-mapping (filter #(and (= role-id (get-in % [:role :id])) (= loc-id (get-in % [:location :id]))) role-mappings)]
                    (data/delete "role-mapping" (:id role-mapping)))
                  (let [role-mappings (filter #(= role-id (get-in % [:role-id])) (data/find-children "user" user-id "role-mapping"))]
                    (doseq [{id :id} role-mappings]
                      (data/delete "role-mapping" id))))
                (data/find-record "user" user-id)))
    :run-if-false forbidden-fn}])

(process/register-processes (map #(DefaultProcess/create %) user-role-processes))
