;; Provides the reusable functions for dealing with our tenancy.
(ns org.healthsciencessc.rpms2.consent-domain.tenancy
  (:require [org.healthsciencessc.rpms2.consent-domain.types :as types]))

(defn only-my-org
  "Returns only items in the collection that belong to the users organization."
  [user coll]
  (let [org-id (get-in user [:organization :id])]
    (filter #(= org-id (get-in % [:organization :id])) coll)))

(defn only-base-org
  "Returns only items in the collection that belong to the base organization."
  [coll]
  (filter #(= types/code-base-org (get-in % [:organization :code])) coll))

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