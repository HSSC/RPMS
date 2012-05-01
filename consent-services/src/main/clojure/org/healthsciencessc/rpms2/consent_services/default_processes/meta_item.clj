(ns org.healthsciencessc.rpms2.consent-services.default-processes.meta-item
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils :only (forbidden-fn)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-domain.roles :as role])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def meta-item-proceses
  [{:name "get-library-metaitems"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])]
                     (or (role/consent-collector? user) (role/protocol-designer? user))))
    :run-fn (fn [params]
              (let [user (get-in params [:session :current-user])
                    user-org-id (get-in user [:organization :id])]
                (data/find-children "organization" user-org-id "meta-item")))
    :run-if-false forbidden-fn}

   {:name "get-library-metaitem"
    :runnable-fn (fn [params]
                   (let [meta-item-id (get-in params [:query-params :metaitem])
                         user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])]
                     (data/belongs-to? "meta-item" meta-item-id "organization" user-org-id)))
    :run-fn (fn [params]
              (let [meta-item-id (get-in params [:query-params :meta-item])]
                (data/find-record "meta-item" meta-item-id)))
    :run-if-false forbidden-fn}

   {:name "put-library-metaitem"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         item-org-id (get-in params [:body-params :organization :id])]
                     (and (role/protocol-designer? user)
                          (= user-org-id item-org-id))))
    :run-fn (fn [params]
              (let [meta-item (:body-params params)]
                (data/create "meta-item" meta-item)))
    :run-if-false forbidden-fn}

   {:name "post-library-metaitem"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         item-org-id (get-in params [:body-params :organization :id])]
                     (and (role/protocol-designer? user)
                          (= user-org-id item-org-id))))
    :run-fn (fn [params]
              (let [meta-item-id (get-in params [:query-params :metaitem])
                    meta-item (:body-params params)]
                (data/update "meta-item" meta-item-id meta-item)))
    :run-if-false forbidden-fn}

   {:name "delete-library-metaitem"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         item-org-id (get-in params [:body-params :organization :id])]
                     (and (role/protocol-designer? user)
                          (= user-org-id item-org-id))))
    :run-fn (fn [params]
              (let [meta-item-id (get-in params [:query-params :metaitem])]
                (data/delete "meta-item" meta-item-id)))
    :run-if-false forbidden-fn}])