(ns org.healthsciencessc.rpms2.consent-collector.config
  (:use [pliant.configure.props]
        [pliant.configure.sniff]))

(def config
    "See http://wiki.healthsciencessc.org/display/Projects/Application+Configuration"
    (slurp-config "consent-collector.props"
     (sniff "RPMSKEY") ))

