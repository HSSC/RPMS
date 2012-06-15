(ns org.healthsciencessc.rpms2.consent-services.default-processes.meta-item
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils :only (forbidden-fn)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.utils :as utils]
            [org.healthsciencessc.rpms2.consent-domain.lookup :as lookup]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
            [org.healthsciencessc.rpms2.consent-domain.runnable :as runnable])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def meta-item-processes
  [{:name "get-library-meta-items"
    :runnable-fn (runnable/gen-designer-org-check utils/current-user utils/lookup-organization)
    :run-fn (utils/gen-type-records-by-org types/meta-item)
    :run-if-false forbidden-fn}

   {:name "get-library-meta-item"
    :runnable-fn (runnable/gen-designer-record-check utils/current-user utils/get-meta-item-record)
    :run-fn utils/get-meta-item-record
    :run-if-false forbidden-fn}

   {:name "put-library-meta-item"
    :runnable-fn (runnable/gen-designer-org-check utils/current-user lookup/get-organization-in-body)
    :run-fn (utils/gen-type-create types/meta-item)
    :run-if-false forbidden-fn}

   {:name "post-library-meta-item"
    :runnable-fn (runnable/gen-designer-record-check utils/current-user utils/get-meta-item-record)
    :run-fn (utils/gen-type-update types/meta-item lookup/get-meta-item-in-query)
    :run-if-false forbidden-fn}

   {:name "delete-library-meta-item"
    :runnable-fn (runnable/gen-designer-record-check utils/current-user utils/get-meta-item-record)
    :run-fn (utils/gen-type-delete types/meta-item lookup/get-meta-item-in-query)
    :run-if-false forbidden-fn}])

(process/register-processes (map #(DefaultProcess/create %) meta-item-processes))