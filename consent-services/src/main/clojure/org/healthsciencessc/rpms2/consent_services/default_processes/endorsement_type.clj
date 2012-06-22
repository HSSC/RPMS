(ns org.healthsciencessc.rpms2.consent-services.default-processes.endorsement-type
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils :only (forbidden-fn admin? super-admin?)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-services.utils :as utils]
            [org.healthsciencessc.rpms2.consent-domain.lookup :as lookup]
            [org.healthsciencessc.rpms2.consent-domain.roles :as roles]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
            [org.healthsciencessc.rpms2.consent-domain.runnable :as runnable])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(defn assign-endorsement-type
  [ctx]
  (let [endorsement-id (lookup/get-endorsement-in-query ctx)
        new-type-id (get-in ctx [:query-params :assign-type])
        endorsement-type-id (lookup/get-endorsement-type-in-query ctx)]
    (data/re-relate-records types/endorsement endorsement-id types/endorsement-type endorsement-type-id new-type-id)))

(defn- authorize-read-node
  [ctx]
  (let [user (utils/current-user ctx)]
    (and (roles/protocol-designer? user)
         (utils/record-belongs-to-user-org ctx types/endorsement-type))))

(defn- authorize-change-node
  [ctx]
  (let [user (utils/current-user ctx)]
    (and (roles/protocol-designer? user)
         (utils/record-owned-by-user-org ctx types/endorsement-type))))

(def endorsement-type-processes
  [{:name "get-library-endorsement-types"
    :runnable-fn (runnable/gen-designer-org-check utils/current-user utils/lookup-organization)
    :run-fn (utils/gen-type-records-by-org types/endorsement-type)
    :run-if-false forbidden-fn}

   {:name "get-library-endorsement-type"
    :runnable-fn authorize-read-node
    :run-fn utils/get-endorsement-type-record
    :run-if-false forbidden-fn}

   {:name "put-library-endorsement-type"
    :runnable-fn (runnable/gen-designer-org-check utils/current-user lookup/get-organization-in-body)
    :run-fn (utils/gen-type-create types/endorsement-type)
    :run-if-false forbidden-fn}

   {:name "post-library-endorsement-type"
    :runnable-fn authorize-change-node
    :run-fn (utils/gen-type-update types/endorsement-type lookup/get-endorsement-type-in-query)
    :run-if-false forbidden-fn}

   {:name "delete-library-endorsement-type"
    :runnable-fn authorize-change-node
    :run-fn (utils/gen-type-delete types/endorsement-type lookup/get-endorsement-type-in-query)
    :run-if-false forbidden-fn}
   
   {:name "post-library-endorsement-type-assign"
    :runnable-fn (runnable/gen-designer-record-check utils/current-user utils/get-endorsement-record)
    :run-fn assign-endorsement-type
    :run-if-false forbidden-fn}])

(process/register-processes (map #(DefaultProcess/create %) endorsement-type-processes))