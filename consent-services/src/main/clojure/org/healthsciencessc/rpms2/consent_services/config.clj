(ns org.healthsciencessc.rpms2.consent-services.config
  (:require [org.healthsciencessc.rpms2.consent-domain.props :as props]))

(def conf
  (props/slurp-config "consent-services.props"))
