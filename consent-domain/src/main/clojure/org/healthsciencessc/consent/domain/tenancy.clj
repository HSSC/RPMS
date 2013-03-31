;; Provides the reusable functions for dealing with our tenancy.
(ns org.healthsciencessc.consent.domain.tenancy
  (:require [org.healthsciencessc.consent.domain.types :as types]
            [clojure.string]))

(defn only-my-org
  "Returns only items in the collection that belong to the users organization."
  [user coll]
  (let [org-id (get-in user [:organization :id])]
    (filter #(= org-id (get-in % [:organization :id])) coll)))

(defn only-my-org?
  "Returns true if all the items in the collection belong only to the users organization."
  [user coll]
  (= (count coll) (count (only-my-org user coll))))

(defn only-base-org
  "Returns only items in the collection that belong to the base organization."
  [coll]
  (filter #(= types/code-base-org (get-in % [:organization :code])) coll))

(defn only-base-org?
  "Returns true if all the items in the collection belong to the base organization."
  [coll]
  (= (count coll) (count (only-base-org coll))))

(defn belongs-to-base?
  "Checks if a data map represents a type that is owned by the base organization."
  [data]
  (= types/code-base-org (get-in data [:organization :code])))

(defn label-for
  "Looks for the first valid value within the organization and location for a specific label."
  [label location organization default]
  (let [labels [(label location)
                (label (:organization location))
                (label organization)
                default]]
    (first (filter #(and (identity %) (not (clojure.string/blank? %))) labels))))

(defn label-for-location
  "Shortcut that looks for the Location label within a location and organization.  Defaults to 'Location'"
  [location organization]
  (label-for :location-label location organization "Location"))

(defn label-for-protocol
  "Shortcut that looks for the Protocol label within a location and organization.  Defaults to 'Protocol'"
  [location organization]
  (label-for :protocol-label location organization "Protocol"))

(defn label-for-consenter
  "Shortcut that looks for the Consenter label within a location and organization.  Defaults to 'Consenter'"
  [location organization]
  (label-for :consenter-label location organization "Consenter"))

(defn label-for-encounter
  "Shortcut that looks for the Encounter label within a location and organization.  Defaults to 'Encounter'"
  [location organization]
  (label-for :encounter-label location organization "Encounter"))