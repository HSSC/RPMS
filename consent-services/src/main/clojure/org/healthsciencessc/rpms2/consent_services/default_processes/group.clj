(ns org.healthsciencessc.rpms2.consent-services.default-processes.group
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils :only (admin? super-admin? some-kind-of-admin? forbidden-fn)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def group-processes
  [{:name "get-security-groups"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         org-id (get-in params [:query-params :organization])
                         loc-id (get-in params [:query-params :location])]
                     (or (super-admin? user)
                         (and (admin? user)
                              (cond
                               org-id (data/belongs-to? "user" (:id user) "organization" org-id)
                               loc-id (data/belongs-to? "location" loc-id "organization" user-org-id)
                              :else true)))))
    :run-fn (fn [params]
              (let [user (get-in params [:session :current-user])
                    user-org-id (get-in user [:organization :id])
                    org-id (get-in params [:query-params :organization])
                    loc-id (get-in params [:query-params :location])]
                (cond
                 org-id (data/find-children "organization" org-id "group")
                 loc-id (data/find-related-records "location" loc-id (list "role-mapping" "group"))
                 :else (cond
                        (super-admin? user) (data/find-all "group")
                        (admin? user) (data/find-children "organization" user-org-id "group")))))
    :run-if-false forbidden-fn}

   {:name "get-security-group"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         group-id (get-in params [:query-params :group])]
                     (or
                      (super-admin? user)
                      (and (admin? user) (data/belongs-to? "group" group-id "organization" user-org-id)))))
    :run-fn (fn [params]
              (let [group-id (get-in params [:query-params :group])]
                (data/find-record "group" group-id)))
    :run-if-false forbidden-fn}

   {:name "put-security-group"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         group (:body-params params)
                         user-org-id (get-in user [:organization :id])
                         group-org-id (get-in group [:organization :id])]
                     (or (super-admin? user)
                         (and (admin? user) (= user-org-id group-org-id)))))
    :run-fn (fn [params]
              (let [group (:body-params params)]
                (data/create "group" group)))
    :run-if-false forbidden-fn}

   {:name "post-security-group"
    :runnable-fn (fn [params]
                   (let [group-id (get-in params [:query-params :group])
                         user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])]
                     (or (super-admin? user)
                         (and (admin? user) (data/belongs-to? "group" group-id "organization" user-org-id false)))))
    :run-fn (fn [params]
              (let [group-id (get-in params [:query-params :group])
                    group (:body-params params)]
                (data/update "group" group-id group)))
    :run-if-false forbidden-fn}

   {:name "delete-security-group"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         group-id (get-in params [:query-params :group])]
                     (or (super-admin? user)
                         (and (admin? user) (data/belongs-to? "group" group-id "organization" user-org-id false)))))
    :run-fn (fn [params]
              (let [group-id (get-in params [:query-params :group])]
                (data/delete "group" group-id)))
    :run-if-false forbidden-fn}])

(process/register-processes (map #(DefaultProcess/create %) group-processes))
