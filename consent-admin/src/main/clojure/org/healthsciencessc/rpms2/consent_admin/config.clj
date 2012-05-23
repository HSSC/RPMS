;; Provides the configuration of the admin
(ns org.healthsciencessc.rpms2.consent-admin.config
  (:use [org.healthsciencessc.rpms2.consent-domain.props :only [slurp-config]]
        [org.healthsciencessc.rpms2.consent-domain.sniff :only [sniff]]))

;; Defines the var that holds the configuration value map.
(def config (slurp-config "consent-admin.props" (sniff "RPMSPKEY")))
