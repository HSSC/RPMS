(ns org.healthsciencessc.rpms2.consent-services.process-dispatcher
  (:require [org.healthsciencessc.rpms2.consent-services.process :as process]))

(defn find-process
  [process-name params]
  (let [custom-process (process/find-custom-process process-name params)]
    (or custom-process (process/find-default-process process-name params))))

(defn dispatch
  [process-name params]
  (let [process (find-process process-name params)]))