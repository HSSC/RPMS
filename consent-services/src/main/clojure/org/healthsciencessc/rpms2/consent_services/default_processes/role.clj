(ns org.healthsciencessc.rpms2.consent-services.default-processes.role
  (:use [clojure.data.json :only (json-str pprint-json)]
        [org.healthsciencessc.rpms2.consent-services.domain-utils :only (admin? super-admin? some-kind-of-admin? forbidden-fn)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def role-processes
  [{:name "get-security-roles"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         org-id (get-in params [:query-params :organization])
                         loc-id (get-in params [:query-params :location])]
                     (or (super-admin? user)
                         (and (admin? user)
                              (cond
                               org-id (data/belongs-to? "user" (:id user) "organization" org-id)
                               loc-id (data/siblings? {:start-type "user" :start-id (:id user) :parent-type "organization" :parent-id user-org-id :sibling-type "location" :sibling-id loc-id}))
                              :else true))))
    :run-fn (fn [params]
              (let [user (get-in params [:session :current-user])
                    user-org-id (get-in user [:organization :id])
                    org-id (get-in params [:query-params :organization])
                    loc-id (get-in params [:query-params :location])]
                (cond
                 org-id (json-str (data/find-children "organization" org-id "role"))
                 loc-id (json-str (data/find-related-records "location" loc-id "role-mapping" "role"))
                 :else (cond
                        (super-admin? user) (json-str (data/find-all "role"))
                        (admin? user) (json-str (data/find-siblings {:start-type "user" :start-id (:id user) :parent-type "organization" :parent-id user-org-id :sibling-type "role"}))))))
    :run-if-false forbidden-fn}

   {:name "get-security-role"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         role-id (get-in params [:query-params :role])]
                     (or
                      (super-admin? user)
                      (and (admin? user) (data/belongs-to? "role" role-id "organization" user-org-id)))))
    :run-fn (fn [params]
              (let [role-id (get-in params [:query-params :role])]
                (json-str (data/find-record "role" role-id))))
    :run-if-false forbidden-fn}

   {:name "put-security-role"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         role (:body-params params)
                         user-org-id (get-in user [:organization :id])
                         role-org-id (get-in role [:organization :id])]
                     (or (super-admin? user)
                         (and (admin? user) (= user-org-id role-org-id)))))
    :run-fn (fn [params]
              (let [role (:body-params params)]
                (json-str (data/create "role" role))))
    :run-if-false forbidden-fn}

   {:name "post-security-role"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         role (:body-params params)
                         user-org-id (get-in user [:organization :id])
                         role-org-id (get-in role [:organization :id])]
                     (or (super-admin? user)
                         (and (admin? user) (= user-org-id role-org-id)))))
    :run-fn (fn [params]
              (let [role-id (get-in params [:query-params :role])
                    role (:body-params params)]
                (json-str (data/update "role" role-id role))))
    :run-if-false forbidden-fn}

   {:name "delete-security-role"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         role-id (get-in params [:query-params :role])]
                     (or (super-admin? user)
                         (and (admin? user) (data/belongs-to? "role" role-id "organization" user-org-id)))))
    :run-fn (fn [params]
              (let [role-id (get-in params [:query-params :role])]
                (json-str (data/delete "role" role-id))))
    :run-if-false forbidden-fn}])

(process/register-processes (map #(DefaultProcess/create %) role-processes))