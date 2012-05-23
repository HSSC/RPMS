(ns org.healthsciencessc.rpms2.consent-admin.test.config
  (:use [org.healthsciencessc.rpms2.consent-admin.config])
  (:use [clojure.test]))

;; Tests that the config is loaded as a map.
(deftest test-config
  (is (map? config)))
