(ns org.healthsciencessc.rpms2.consent-services.default-processes.user
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils :only (admin? super-admin? some-kind-of-admin? forbidden-fn)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-services.auth :as auth])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def user-processes
  [{:name "authenticate"
    :runnable-fn (fn [params] true)
    :run-fn (fn [{:keys [username password]}]
              (if-let [user-node (first (filter #(= username (:username %))
                                                (data/get-raw-nodes "user")))]
                (if (and password user-node (auth/good-password? password (:password user-node)))
                  (first (data/find-records-by-attrs "user" {:username username})))))}

   {:name "get-security-authenticate"
    :runnable-fn (fn [params] true)
    :run-fn (fn [params]
              (:current-user (:session params)))
    :run-if-false forbidden-fn}

   {:name "get-security-users"
    :runnable-fn (fn [params]
                   (let [current-user (get-in params [:session :current-user])
                         current-user-org-id (get-in current-user [:organization :id])
                         org-id (get-in params [:query-params :organization])]
                     (or (super-admin? current-user)
                         (and (admin? current-user)
                              (if org-id (data/belongs-to? "user" (:id current-user) "organization" org-id) true)))))
    :run-fn (fn [params]
              (let [user (get-in params [:session :current-user])
                    user-org-id (get-in user [:organization :id])
                    org-id (get-in params [:query-params :organization])]
                (cond
                 org-id (data/find-children "organization" org-id "user")
                 :else (cond
                        (super-admin? user) (data/find-all "user")
                        (admin? user) (data/find-children "organization" user-org-id "user")))))
    :run-if-false forbidden-fn}

   {:name "get-security-user"
    :runnable-fn (fn [params]
                   (let [current-user (get-in params [:session :current-user])
                         current-user-org-id (get-in current-user [:organization :id])
                         user-id (get-in params [:query-params :user])]
                     (or
                      (super-admin? current-user)
                      (and (admin? current-user) (data/belongs-to? "user" user-id "organization" current-user-org-id)))))
    :run-fn (fn [params]
              (let [user-id (get-in params [:query-params :user])]
                (data/find-record "user" user-id)))
    :run-if-false forbidden-fn}

   {:name "put-security-user"
    :runnable-fn (fn [params]
                   (let [current-user (get-in params [:session :current-user])
                         current-user-org-id (get-in current-user [:organization :id])
                         user (:body-params params)
                         user-org-id (get-in user [:organization :id])]
                     (or (super-admin? user)
                         (and (admin? user) (= current-user-org-id user-org-id)))))
    :run-fn (fn [params]
              (let [user-data (:body-params params)
                    unhashed-pwd (:password user-data)
                    user (assoc user-data :password (auth/hash-password unhashed-pwd))]
                (data/create "user" user)))
    :run-if-false forbidden-fn}

   {:name "post-security-user"
    :runnable-fn (fn [params]
                   (let [current-user (get-in params [:session :current-user])
                         current-user-org-id (get-in current-user [:organization :id])
                         org-id (get-in params [:query-params :organization])]
                     (or (super-admin? current-user)
                         (and (admin? current-user)
                              (if org-id (data/belongs-to? "user" (:id current-user) "organization" org-id) true)))))
    :run-fn (fn [params]
              (let [user-id (get-in params [:query-params :user])
                    user-data (:body-params params)]
                (data/update "user" user-id user-data)))
    :run-if-false forbidden-fn}

   {:name "delete-security-user"
    :runnable-fn (fn [params]
                   (let [current-user (get-in params [:session :current-user])
                         current-user-org-id (get-in current-user [:organization :id])
                         user-id (get-in params [:query-params :user])]
                     (or (super-admin? current-user)
                         (and (admin? current-user) (data/belongs-to? "user" user-id "organization" current-user-org-id)))))
    :run-fn (fn [params]
              (let [user-id (get-in params [:query-params :user])]
                (data/delete "user" user-id)))
    :run-if-false forbidden-fn}])

(process/register-processes (map #(DefaultProcess/create %) user-processes))
