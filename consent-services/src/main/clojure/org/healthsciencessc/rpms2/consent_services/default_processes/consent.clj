(ns org.healthsciencessc.rpms2.consent-services.default-processes.consent
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils
         :only (forbidden-fn)]
        [org.healthsciencessc.rpms2.consent-domain.roles]
        [ring.util.response :only (not-found response status)])
  (:require [clojure.walk :as walk]
            [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-domain.core :as domain]
            [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-domain.runnable :as runnable]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
            [org.healthsciencessc.rpms2.consent-services.utils :as utils])
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
  (if (seq regex-map)
    (let [regex-fn (fn [[k v]]
                     (let [rgx (regex-insensitive v)]
                       (fn [m]
                         (if (string? (get m k))
                           (re-find rgx (get m k))))))
          juxt-fn (apply juxt (map regex-fn regex-map))]
      (fn [x]
        (every? identity (juxt-fn x))))
    (constantly true)))

(defn can-see-consenters? [params]
  (let [user (get-in params [:session :current-user])
        user-org-id (get-in user [:organization :id])]
    (or
      (consent-collector? user :organization {:id user-org-id})
      (consent-manager? user :organization {:id user-org-id}))))

(defn get-encounter-ids
  [consent-encounter-data]
  (flatten (for [[type type-coll] consent-encounter-data]
            (for [record type-coll] (get-in record [:encounter :id])))))

(defn have-same-encounter?
  [data]
  (let [encounter-ids (get-encounter-ids data)]
    (and (every? #(not (nil? %)) encounter-ids)
         (= 1 (count (distinct encounter-ids))))))

(defn can-see-location-consents?
  [location-id user]
  (or
    (consent-collector? user :location {:id location-id})
    (consent-manager? user :location {:id location-id})))

(defn- get-consent-locations
  [ctx]
  (let [user (get-in ctx [:session :current-user])
        location-ids (get-in ctx [:query-params :location])]
    (cond
      (coll? location-ids)
        (filter #(can-see-location-consents? % user) location-ids)
      location-ids
        [(can-see-location-consents? location-ids user)]
      :else
        (for [mapping (consent-manager-mappings user)]
          (get-in mapping [:location :id])))))
  
(defn set-protocols
  [encounters]
  (let [version-refs (apply concat (for [e encounters] (concat (:consents e) 
                                                               (:consent-endorsements e) 
                                                               (:consent-meta-items e))))
        version-ids (filter identity (distinct (map #(get-in % [:protocol-version :id]) version-refs)))
        protocol-map (into {} (for [id version-ids]
                                [id (first (data/find-related-records types/protocol-version id [types/protocol]))]))]
    (walk/postwalk 
      (fn [o] (if (protocol-map (:id o))
                (assoc o :protocol (protocol-map (:id o)))
                o)) encounters)))

(defn get-consenter-consents
  [ctx]
  (if-let [consenter-id (get-in ctx [:query-params :consenter])]
    (if-let [location-ids (get-consent-locations ctx)]
      (let [consenter (data/find-record types/consenter consenter-id)
            encounters (data/find-children types/consenter consenter-id types/encounter)]
        (assoc consenter :encounters (set-protocols encounters)))
      (status (response {:message "Consenter can not view consents for any location."}) 400))
    (status (response {:message "Must provide a consenter ID."}) 400)))

(def consent-processes
  ;; curl -i -X GET -H "Content-type: application/json" http://localhost:3000/consent/consenters?organization=<ID>
  [{:name "get-consent-consenter"
    :runnable-fn can-see-consenters?
    :run-fn (fn [params]
              (if-let  [consenter-id (get-in params [:query-params :consenter])]
                (let [org-id (get-in params [:session :current-user :organization :id])
                      consenter (data/find-record "consenter" consenter-id)]
                  (if (= org-id (get-in consenter [:organization :id]))
                    consenter
                    (not-found "")))))
    :run-if-false forbidden-fn}

   {:name "put-consent-consenter"
    :runnable-fn can-see-consenters?
    ;;FIXME this isn't validating org-id of what was passed in
    :run-fn (fn [params]
              (let [org (get-in params [:session :current-user :organization])
                    loc-id (get-in params [:query-params :location])
                    consenter (assoc (:body-params params) :organization org)]
                (data/create "consenter" consenter)))
    :run-if-false forbidden-fn}

   {:name "get-consent-consenters"
    :runnable-fn can-see-consenters?
    :run-fn (fn [params]
              (let [org-id (get-in params [:session :current-user :organization :id])
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
                  (not-found ""))))
    :run-if-false forbidden-fn}

   {:name "put-consent-collect"
    :runnable-fn (fn [params]
                (let [current-user (utils/current-user params)
                      encounter-consent-data (:body-params params)
                      encounter-id (first (get-encounter-ids encounter-consent-data))
                      encounter (data/find-record types/encounter encounter-id)]
                  (and (have-same-encounter? encounter-consent-data)
                       (runnable/can-collect-location current-user (:location encounter)))))
    :run-fn (fn [params]
              (let [encounter-consent-data (:body-params params)
                    encounter-id (first (get-encounter-ids encounter-consent-data))]
                (data/create-records "encounter" (assoc encounter-consent-data :id encounter-id))))
    :run-if-false forbidden-fn}
   
   {:name "get-consents"
    :runnable-fn can-see-consenters?
    :run-fn get-consenter-consents
    :run-if-false forbidden-fn}])

(process/register-processes (map #(DefaultProcess/create %) consent-processes))
