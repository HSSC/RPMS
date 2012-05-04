(ns org.healthsciencessc.rpms2.consent-services.default-processes.policy
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils :only (forbidden-fn)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-domain.roles :as role])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def policy-processes
  [{:name "get-library-policys"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])]
                     (or (role/consent-collector? user) (role/protocol-designer? user))))
    :run-fn (fn [params]
              (let [user (get-in params [:session :current-user])
                    user-org-id (get-in user [:organization :id])]
                (data/find-children "organization" user-org-id "policy")))
    :run-if-false forbidden-fn}

   {:name "get-library-policy"
    :runnable-fn (fn [params]
                   (let [policy-id (get-in params [:query-params :policy])
                         user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])]
                     (data/belongs-to? "policy" policy-id "organization" user-org-id)))
    :run-fn (fn [params]
              (let [policy-id (get-in params [:query-params :policy])]
                (data/find-record "policy" policy-id)))
    :run-if-false forbidden-fn}

   {:name "put-library-policy"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         item-org-id (get-in params [:body-params :organization :id])]
                     (and (role/protocol-designer? user)
                          (= user-org-id item-org-id))))
    :run-fn (fn [params]
              (let [policy (:body-params params)]
                (data/create "policy" policy)))
    :run-if-false forbidden-fn}

   {:name "post-library-policy"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         item-org-id (get-in params [:body-params :organization :id])]
                     (and (role/protocol-designer? user)
                          (= user-org-id item-org-id))))
    :run-fn (fn [params]
              (let [policy-id (get-in params [:query-params :policy])
                    policy (:body-params params)]
                (data/update "policy" policy-id policy)))
    :run-if-false forbidden-fn}

   {:name "delete-library-policy"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         item-org-id (get-in params [:body-params :organization :id])]
                     (and (role/protocol-designer? user)
                          (= user-org-id item-org-id))))
    :run-fn (fn [params]
              (let [policy-id (get-in params [:query-params :policy])]
                (data/delete "policy" policy-id)))
    :run-if-false forbidden-fn}])

(process/register-processes (map #(DefaultProcess/create %) policy-processes))