(ns org.healthsciencessc.rpms2.consent-services.config
  (:use [org.healthsciencessc.rpms2.consent-domain.props :only [slurp-config]]
        [org.healthsciencessc.rpms2.consent-domain.sniff :only [sniff]]))

(def conf
  (slurp-config "consent-services.props" (sniff "RPMSPKEY")))
