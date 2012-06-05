(ns org.healthsciencessc.rpms2.consent-admin.auth.protocol
  (:require [org.healthsciencessc.rpms2.consent-admin.security :as security]
            [org.healthsciencessc.rpms2.consent-domain.roles :as roles]
            [org.healthsciencessc.rpms2.consent-domain.runnable :as runnable]
            [org.healthsciencessc.rpms2.consent-admin.services :as services])
  (:use [clojure.tools.logging :only (info error)]))

(defn auth-protocol-id
  [protocol-id & contraints]
  (let [protocol (services/get-protocol protocol-id)
        user (security/current-user)]
    (and (runnable/can-design-protocol user protocol)
         (every? #(% protocol) contraints))))

(defn auth-protocol-version-id
  [protocol-version-id & contraints]
  (let [protocol-version (services/get-protocol-version protocol-version-id)
        protocol (:protocol protocol-version)
        user (security/current-user)]
    (and (runnable/can-design-protocol user protocol)
         (every? #(% protocol-version) contraints))))