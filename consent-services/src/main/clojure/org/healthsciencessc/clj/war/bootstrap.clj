(ns org.healthsciencessc.clj.war.bootstrap
  (:require [org.healthsciencessc.rpms2.consent-services.core :as core]))

;; Bootrap a pointer to the real routes.
(def app core/app)