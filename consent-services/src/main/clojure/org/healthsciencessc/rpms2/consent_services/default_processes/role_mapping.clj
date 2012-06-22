(ns org.healthsciencessc.rpms2.consent-services.default-processes.role-mapping
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils :only (forbidden-fn)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-domain.roles :as roles]
            [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-services.utils :as utils])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def role-mapping-processes
  [{:name "delete-security-role-mapping"
    :runnable-fn (fn [params]
                   (let [current-user (utils/current-user params)]
                     (or (roles/superadmin? current-user)
                         (let [role-mapping-id (get-in params [:query-params :role-mapping])
                               role-mapping (data/find-record "role-mapping" role-mapping-id)
                               record (if (:user role-mapping) 
                                        (data/find-record "user" (get-in role-mapping [:user :id]))
                                        (data/find-record "group" (get-in role-mapping [:group :id])))]
                           (roles/admin? current-user :organization {:id (get-in record [:organization :id])})))))
    :run-fn (fn [params]
              (let [role-mapping-id (get-in params [:query-params :role-mapping])]
                (data/delete "role-mapping" role-mapping-id)))
    :run-if-false forbidden-fn}])

(process/register-processes (map #(DefaultProcess/create %) role-mapping-processes))
