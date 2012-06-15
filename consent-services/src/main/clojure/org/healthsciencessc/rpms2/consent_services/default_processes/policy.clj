(ns org.healthsciencessc.rpms2.consent-services.default-processes.policy
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils :only (forbidden-fn)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.utils :as utils]
            [org.healthsciencessc.rpms2.consent-domain.lookup :as lookup]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
            [org.healthsciencessc.rpms2.consent-domain.runnable :as runnable])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def policy-processes
  [{:name "get-library-policies"
    :runnable-fn (runnable/gen-designer-org-check utils/current-user utils/lookup-organization)
    :run-fn (utils/gen-type-records-by-org types/policy)
    :run-if-false forbidden-fn}

   {:name "get-library-policy"
    :runnable-fn (runnable/gen-designer-record-check utils/current-user utils/get-policy-record)
    :run-fn utils/get-policy-record
    :run-if-false forbidden-fn}

   {:name "put-library-policy"
    :runnable-fn (runnable/gen-designer-org-check utils/current-user lookup/get-organization-in-body)
    :run-fn (utils/gen-type-create types/policy)
    :run-if-false forbidden-fn}

   {:name "post-library-policy"
    :runnable-fn (runnable/gen-designer-record-check utils/current-user utils/get-policy-record)
    :run-fn (utils/gen-type-update types/policy lookup/get-policy-in-query)
    :run-if-false forbidden-fn}

   {:name "delete-library-policy"
    :runnable-fn (runnable/gen-designer-record-check utils/current-user utils/get-policy-record)
    :run-fn (utils/gen-type-delete types/policy lookup/get-policy-in-query)
    :run-if-false forbidden-fn}])

(process/register-processes (map #(DefaultProcess/create %) policy-processes))