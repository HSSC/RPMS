(ns org.healthsciencessc.rpms2.consent-services.default-processes.consent
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils
         :only (forbidden-fn)]
        [org.healthsciencessc.rpms2.consent-domain.roles])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-domain.core :as domain]
            [org.healthsciencessc.rpms2.consent-services.data :as data])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]
           [java.util.regex Pattern]))

;; filter helper
(defn filter-by [k v xs]
  (filter #(= v (get-in % [k :id])) xs))

;; regex helpers
(def ^:const case-insensitive
  (bit-or Pattern/CASE_INSENSITIVE
          Pattern/UNICODE_CASE))

(defn regex-insensitive
  [str]
  (Pattern/compile str case-insensitive))

(defn regex-map-pred
  "This function takes a map where values are (string) regexes to be matched against,
   and returns a predicate that applies to a map, only returning true when all
   [key regex] pairs successfully match.  Uses case insensitive by default.
   ((regex-map-pred {:a \"abc\" :b \"321\"}) {:a \"aa123ABC\" :b \"1122321\"}) ->> true"
  [regex-map]
  (let [regex-fn (fn [[k v]]
                   (let [rgx (regex-insensitive v)]
                     (fn [m]
                       (if (string? (get m k))
                         (re-find rgx (get m k))))))
        juxt-fn (apply juxt (map regex-fn regex-map))]
    (fn [x]
      (every? identity (juxt-fn x)))))

(defn can-see-consenters? [params]
  (let [user (get-in params [:session :current-user])
        user-org-id (get-in user [:organization :id])
        org-id (get-in params [:query-params :organization])]
    (or
     (superadmin? user)
     (and (or
           (admin? user :organization {:id org-id})
           (consent-collector? user :organization {:id org-id})
           (consent-manager? user :organization {:id org-id}))
          (= user-org-id org-id)))))

(def consent-processes
  ;; curl -i -X GET -H "Content-type: application/json" http://localhost:3000/consent/consenters?organization=<ID>
  [{:name "get-consent-consenters"
    :runnable-fn can-see-consenters?
    :run-fn (fn [params]
              (let [org-id (get-in params [:query-params :organization])
                    loc-id (get-in params [:query-params :location])
                    consenters (data/find-children "organization" org-id "consenter")]
                (if loc-id
                  (filter-by :location loc-id consenters)
                  consenters)))
    :run-if-false forbidden-fn}

   {:name "put-consent-consenter"
    :runnable-fn can-see-consenters?
    ;;FIXME this isn't validating org-id of what was passed in
    :run-fn (fn [params]
              (let [org-id (get-in params [:query-params :organization])
                    loc-id (get-in params [:query-params :location])
                    consenter (:body-params params)]
                (data/create "consenter" consenter)))
    :run-if-false forbidden-fn}

   {:name "get-consent-consenter"
    :runnable-fn can-see-consenters?
    :run-fn (fn [params]
              (let [org-id (get-in params [:query-params :organization])
                    loc-id (get-in params [:query-params :location])
                    consenters (data/find-children "organization" org-id "consenter")
                    search-keys (select-keys (:query-params params)
                                             (keys (get-in domain/default-data-defs
                                                           ["consenter" :attributes])))
                    regex-match? (regex-map-pred search-keys)
                    loc-consenters (if loc-id
                                     (filter-by :location loc-id consenters)
                                     consenters)
                    results (filter regex-match? loc-consenters)]
                (if (< 0 (count results))
                  (filter regex-match? loc-consenters)
                  {:status 404})))
    :run-if-false forbidden-fn}])

(process/register-processes (map #(DefaultProcess/create %) consent-processes))
