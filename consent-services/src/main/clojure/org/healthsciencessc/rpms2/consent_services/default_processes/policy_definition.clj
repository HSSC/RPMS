(ns org.healthsciencessc.rpms2.consent-services.default-processes.policy-definition
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils :only (forbidden-fn)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.utils :as utils]
            [org.healthsciencessc.rpms2.consent-domain.lookup :as lookup]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
            [org.healthsciencessc.rpms2.consent-domain.runnable :as runnable])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def policy-definition-processes
  [{:name "get-library-policy-definitions"
    :runnable-fn (runnable/gen-designer-org-check utils/current-user utils/lookup-organization)
    :run-fn (utils/gen-type-records-by-org types/policy-definition)
    :run-if-false forbidden-fn}

   {:name "get-library-policy-definition"
    :runnable-fn (runnable/gen-designer-record-check utils/current-user utils/get-policy-definition-record)
    :run-fn utils/get-policy-definition-record
    :run-if-false forbidden-fn}

   {:name "put-library-policy-definition"
    :runnable-fn (runnable/gen-designer-org-check utils/current-user lookup/get-organization-in-body)
    :run-fn (utils/gen-type-create types/policy-definition)
    :run-if-false forbidden-fn}

   {:name "post-library-policy-definition"
    :runnable-fn (runnable/gen-designer-record-check utils/current-user utils/get-policy-definition-record)
    :run-fn (utils/gen-type-update types/policy-definition lookup/get-policy-definition-in-query)
    :run-if-false forbidden-fn}

   {:name "delete-library-policy-definition"
    :runnable-fn (runnable/gen-designer-record-check utils/current-user utils/get-policy-definition-record)
    :run-fn (utils/gen-type-delete types/policy-definition lookup/get-policy-definition-in-query)
    :run-if-false forbidden-fn}])

(process/register-processes (map #(DefaultProcess/create %) policy-definition-processes))