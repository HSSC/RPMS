(ns org.healthsciencessc.rpms2.consent-services.core
  (:require [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.process-engine.web-service :as process-ws]))

(def app
  process-ws/processes)

;; (data/connect)
(process/load-processes "resources/process_definitions")