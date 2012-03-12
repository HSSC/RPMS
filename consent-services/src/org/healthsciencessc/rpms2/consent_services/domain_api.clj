(ns org.healthsciencessc.rpms2.consent-services.domain-api
  (:require [org.healthsciencessc.rpms2.consent-services.data :as data])
  (:import [org.healthsciencessc.rpms2.consent_domain.core Organization]))

(defn find-all-organizations
  []
  (data/find-all "Organization"))

(defn find-organization
  [id]
  (data/find-record id))

(defn create-organization
  [params]
  (data/create (Organization/create params)))

(defn update-organization
  [params]
  (data/update (Organization/create params)))
