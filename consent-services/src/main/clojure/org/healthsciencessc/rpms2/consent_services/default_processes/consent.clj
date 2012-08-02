(ns org.healthsciencessc.rpms2.consent-services.default-processes.consent
  (:use     [pliant.process :only [defprocess as-method]])
  (:require [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-services.respond :as respond]
            [org.healthsciencessc.rpms2.consent-services.session :as session]
            [org.healthsciencessc.rpms2.consent-services.vouch :as vouch]
            [org.healthsciencessc.rpms2.consent-domain.core :as domain]
            [org.healthsciencessc.rpms2.consent-domain.roles :as roles]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint]
            [clojure.walk :as walk])
  (:import  [java.util.regex Pattern]))

(defn consent-locations
  [ctx]
  (let [user (session/current-user ctx)
        mappings (concat (roles/consent-manager-mappings user) (roles/consent-collector-mappings user))]
    (distinct (filter identity (for [m mappings] (:location m))))))

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

(defn filter-consenters
  [ctx consenters]
  (let[search-keys (select-keys (:query-params ctx)
                                (keys (get-in domain/default-data-defs
                                              ["consenter" :attributes])))
       regex-match? (regex-map-pred search-keys)
       results (filter regex-match? consenters)]
    (if (< 0 (count results))
      results
      (respond/not-found "No consenters were found."))))


(defprocess get-consenters
  [ctx]
  (let [location-id (get-in ctx [:query-params :location])
        type (if location-id types/location types/organization)
        record (if location-id (vouch/collects-or-manages-location ctx) (vouch/collects-or-manages ctx))]
    (if record
      (filter-consenters ctx (data/find-children type (:id record) types/consenter))
      (respond/forbidden))))

(as-method get-consenters endpoint/endpoints "get-consent-consenters")

(defprocess get-consenter
  [ctx]
  (let [consenter (vouch/collects-or-manages-consenter ctx)]
    (if consenter
      consenter
      (respond/forbidden))))

(as-method get-consenter endpoint/endpoints "get-consent-consenter")

(defprocess add-consenter
  [ctx]
  (let [location (vouch/collects-location ctx)]
    (if location
      (let [org (:organization location)
            data (assoc (:body-params ctx) :organization org :location location)]
        (data/create types/consenter data))
      (respond/forbidden))))

(as-method add-consenter endpoint/endpoints "put-consent-consenter")

(defprocess add-consents
  [ctx]
  (let [encounter (vouch/collects-encounter ctx)]
    (if encounter
      (let [location (:location encounter)
            org (:organization location)
            data (assoc (:body-params ctx) :id (:id encounter))]
        (data/create-records types/encounter data))
      (respond/forbidden))))

(as-method add-consents endpoint/endpoints "put-consent-collect")
(as-method add-consents endpoint/endpoints "put-consents")

(defprocess get-consents
  [ctx]
  (let [consenter (vouch/collects-or-manages-consenter ctx)]
    (if consenter
      (let [location-ids (into #{} (map :id (consent-locations ctx)))
            encounters (data/find-children types/consenter (:id consenter) types/encounter)
            authed-encounters (filter #(location-ids (get-in % [:location :id]))  encounters)]
        (assoc consenter :encounters (set-protocols authed-encounters)))
      (respond/forbidden))))

(as-method get-consents endpoint/endpoints "get-consents")
