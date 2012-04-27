(ns org.healthsciencessc.rpms2.consent-services.default-processes.user-group
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils :only (admin? super-admin? some-kind-of-admin? forbidden-fn)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def user-group-processes
  [{:name "put-security-usergroup"
    :runnable-fn (fn [params]
                   (let [current-user (get-in params [:session :current-user])
                         current-user-org-id (get-in current-user [:organization :id])
                         group-id (get-in params [:query-params :group])
                         user-id (get-in params [:query-params :user])]
                     (and group-id user-id
                          (or (super-admin? current-user)
                              (and (admin? current-user)
                                   (data/belongs-to? "user" user-id "organization" current-user-org-id)
                                   (data/belongs-to? "group" group-id "organization" current-user-org-id))))))
    :run-fn (fn [params]
              (let [user-id (get-in params [:query-params :user])
                    group-id (get-in params [:query-params :group])]
                (data/relate-records "user" user-id "group" group-id)))
    :run-if-false forbidden-fn}
   
   {:name "delete-security-usergroup"
    :runnable-fn (fn [params]
                   (let [current-user (get-in params [:session :current-user])
                         current-user-org-id (get-in current-user [:organization :id])
                         group-id (get-in params [:query-params :group])
                         user-id (get-in params [:query-params :user])]
                     (and group-id user-id
                          (or (super-admin? current-user)
                              (and (admin? current-user)
                                   (data/belongs-to? "user" user-id "organization" current-user-org-id)
                                   (data/belongs-to? "group" group-id "organization" current-user-org-id))))))
    :run-fn (fn [params]
              (let [user-id (get-in params [:query-params :user])
                    group-id (get-in params [:query-params :group])]
                (data/unrelate-records "user" user-id "group" group-id)))
    :run-if-false forbidden-fn}])

(process/register-processes (map #(DefaultProcess/create %) user-group-processes))