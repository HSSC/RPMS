(ns org.healthsciencessc.rpms2.consent-services.default-processes.language
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils :only (forbidden-fn)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-domain.roles :as roles]
            [org.healthsciencessc.rpms2.consent-domain.lookup :as lookup]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
            [org.healthsciencessc.rpms2.consent-domain.runnable :as runnable]
            [org.healthsciencessc.rpms2.consent-services.utils :as utils])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(defn- authorize-read-node
  [ctx]
  (let [user (utils/current-user ctx)]
    (and (roles/protocol-designer? user)
         (utils/record-belongs-to-user-org ctx types/language))))

(def language-processes
  [{:name "get-library-languages"
    :runnable-fn (constantly true) ;; Allow any role to read the list of languages.
    :run-fn (fn [params]
              (let [user (get-in params [:session :current-user])
                    org-id (or (lookup/get-organization-in-query params) (get-in user [:organization :id]))]
                (data/find-children "organization" org-id "language")))
    :run-if-false forbidden-fn}

   {:name "get-library-language"
    :runnable-fn authorize-read-node
    :run-fn utils/get-language-record
    :run-if-false forbidden-fn}

   {:name "put-library-language"
    :runnable-fn (runnable/gen-designer-org-check utils/current-user lookup/get-organization-in-body)
    :run-fn (utils/gen-type-create types/language)
    :run-if-false forbidden-fn}

   {:name "post-library-language"
    :runnable-fn (runnable/gen-designer-record-check utils/current-user utils/get-language-record)
    :run-fn (utils/gen-type-update types/language lookup/get-language-in-query)
    :run-if-false forbidden-fn}

   {:name "delete-library-language"
    :runnable-fn (runnable/gen-designer-record-check utils/current-user utils/get-language-record)
    :run-fn (utils/gen-type-delete types/language lookup/get-language-in-query)
    :run-if-false forbidden-fn}])

(process/register-processes (map #(DefaultProcess/create %) language-processes))