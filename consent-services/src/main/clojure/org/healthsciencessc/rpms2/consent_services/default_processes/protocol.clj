(ns org.healthsciencessc.rpms2.consent-services.default-processes.protocol
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils :only (forbidden-fn current-user)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-domain.roles :as role]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
            [org.healthsciencessc.rpms2.consent-domain.runnable :as runnable])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(defn user-is-designer-for-protocol
  [user protocol-id]
  (let [protocol (data/find-record types/protocol protocol-id)
        location (:location protocol)]
    (role/protocol-designer? user :location {:id (:id location)})))

(def protocol-processes
  [{:name "get-protocols"
    :runnable-fn (runnable/gen-designer-location-check current-user [:query-params :location])
    :run-fn (fn [params]
              (let [loc (get-in params [:query-params :location])]
                (sort-by :name (data/find-children types/location loc types/protocol))))
    :run-if-false forbidden-fn}

   {:name "get-protocol"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         protocol-id (get-in params [:query-params :protocol])]
                     (user-is-designer-for-protocol user protocol-id)))
    :run-fn (fn [params]
              (let [protocol-id (get-in params [:query-params :protocol])]
                (data/find-record types/protocol protocol-id)))
    :run-if-false forbidden-fn}

   {:name "put-protocol"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         item-org-id (get-in params [:body-params :organization :id])]
                     (and (role/protocol-designer? user)
                          (= user-org-id item-org-id))))
    :run-fn (fn [params]
              (let [protocol (:body-params params)]
                (data/create types/protocol protocol)))
    :run-if-false forbidden-fn}

   {:name "post-protocol"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         protocol-id (get-in params [:query-params :protocol])]
                     (user-is-designer-for-protocol user protocol-id)))
    :run-fn (fn [params]
              (let [protocol-id (get-in params [:query-params :protocol])
                    protocol (:body-params params)]
                (data/update types/protocol protocol-id protocol)))
    :run-if-false forbidden-fn}

   {:name "delete-protocol"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         protocol-id (get-in params [:query-params :protocol])]
                     (user-is-designer-for-protocol user protocol-id)))
    :run-fn (fn [params]
              (let [protocol-id (get-in params [:query-params :protocol])]
                (data/delete types/protocol protocol-id)))
    :run-if-false forbidden-fn}])


(process/register-processes (map #(DefaultProcess/create %) protocol-processes))