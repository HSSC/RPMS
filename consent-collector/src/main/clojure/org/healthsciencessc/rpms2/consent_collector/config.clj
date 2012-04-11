(ns org.healthsciencessc.rpms2.consent-collector.config
    (:use [org.healthsciencessc.rpms2.consent-domain.props]
        [org.healthsciencessc.rpms2.consent-domain.sniff]))

(def config
    "See http://wiki.healthsciencessc.org/display/Projects/Application+Configuration"
    (slurp-config "consent-collector.props"
     (sniff "RPMSKEY") ))

