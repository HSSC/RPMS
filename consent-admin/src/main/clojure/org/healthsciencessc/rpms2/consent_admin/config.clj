;; Provides the configuration of the admin
(ns org.healthsciencessc.rpms2.consent-admin.config
  (:use [org.healthsciencessc.rpms2.consent-domain.props :only [slurp-config]]
        [org.healthsciencessc.rpms2.consent-domain.sniff :only [sniff]]
        [clojure.string :only (split blank?)]))

;; Defines the var that holds the configuration value map.
(def config (slurp-config "consent-admin.props" (sniff "RPMSPKEY")))

;; 
(defn bootstrap-locations
  "Returns a collection of classpath locations to scan for additional clojure files to load."
  []
  (let [locs "org/healthsciencessc/rpms2/consent_admin/process"
        coll (config "paths.bootstrap")]
    (if (blank? coll)
      [locs]
      (cons locs (split coll  #"\s+")))))
