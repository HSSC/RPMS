(ns org.healthsciencessc.rpms2.consent-services.default-processes.init
  (:require [org.healthsciencessc.rpms2.consent-services.default-processes.consent :as consent]
            [org.healthsciencessc.rpms2.consent-services.default-processes.group-role :as group-role]
            [org.healthsciencessc.rpms2.consent-services.default-processes.group :as group]
            [org.healthsciencessc.rpms2.consent-services.default-processes.location :as location]
            [org.healthsciencessc.rpms2.consent-services.default-processes.meta-item :as meta-item]
            [org.healthsciencessc.rpms2.consent-services.default-processes.organization :as organization]
            [org.healthsciencessc.rpms2.consent-services.default-processes.policy-definition :as policy-definition]
            [org.healthsciencessc.rpms2.consent-services.default-processes.policy :as policy]
            [org.healthsciencessc.rpms2.consent-services.default-processes.role :as role]
            [org.healthsciencessc.rpms2.consent-services.default-processes.user-group :as user-group]
            [org.healthsciencessc.rpms2.consent-services.default-processes.user :as user]
            [org.healthsciencessc.rpms2.consent-services.default-processes.widget :as widget]))

