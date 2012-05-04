(ns org.healthsciencessc.rpms2.consent-services.default-processes.policy-definition
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils :only (forbidden-fn)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-domain.roles :as role])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def policy-definition-processes
  [{:name "get-library-policydefinitions"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])]
                     (or (role/consent-collector? user) (role/protocol-designer? user))))
    :run-fn (fn [params]
              (let [user (get-in params [:session :current-user])
                    user-org-id (get-in user [:organization :id])]
                (data/find-children "organization" user-org-id "policy-definition")))
    :run-if-false forbidden-fn}

   {:name "get-library-policydefinition"
    :runnable-fn (fn [params]
                   (let [policy-definition-id (get-in params [:query-params :policy-definition])
                         user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])]
                     (data/belongs-to? "policy-definition" policy-definition-id "organization" user-org-id)))
    :run-fn (fn [params]
              (let [policy-definition-id (get-in params [:query-params :policy-definition])]
                (data/find-record "policy-definition" policy-definition-id)))
    :run-if-false forbidden-fn}

   {:name "put-library-policydefinition"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         item-org-id (get-in params [:body-params :organization :id])]
                     (and (role/protocol-designer? user)
                          (= user-org-id item-org-id))))
    :run-fn (fn [params]
              (let [policy-definition (:body-params params)]
                (data/create "policy-definition" policy-definition)))
    :run-if-false forbidden-fn}

   {:name "post-library-policydefinition"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         item-org-id (get-in params [:body-params :organization :id])]
                     (and (role/protocol-designer? user)
                          (= user-org-id item-org-id))))
    :run-fn (fn [params]
              (let [policy-definition-id (get-in params [:query-params :policy-definition])
                    policy-definition (:body-params params)]
                (data/update "policy-definition" policy-definition-id policy-definition)))
    :run-if-false forbidden-fn}

   {:name "delete-library-policydefinition"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         item-org-id (get-in params [:body-params :organization :id])]
                     (and (role/protocol-designer? user)
                          (= user-org-id item-org-id))))
    :run-fn (fn [params]
              (let [policy-definition-id (get-in params [:query-params :policy-definition])]
                (data/delete "policy-definition" policy-definition-id)))
    :run-if-false forbidden-fn}])

(process/register-processes (map #(DefaultProcess/create %) policy-definition-processes))