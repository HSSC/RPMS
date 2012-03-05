(ns org.healthsciencessc.rpms2.consent-services.process-dispatcher
  (:require [org.healthsciencessc.rpms2.consent-services.process :as process]))

(defn find-process
  [name params]
  (let [custom-process (process/find-custom-process name params)]
    (or custom-process (process/find-default-process name params))))

(defn dispatch
  [name params]
  (let [process (find-process name params)]
    (if process
      (process/run process params))))