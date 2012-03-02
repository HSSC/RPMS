(ns org.healthsciencessc.rpms2.consent-services.process
  (:require [org.healthsciencessc.rpms2.consent-services.default-processes :as dp]
            [org.healthsciencessc.rpms2.consent-services.custom-processes :as cp]))

(defprotocol IConsentProcess
  (runnable? [this arg-map])
  (run [this arg-map]))

(defrecord ConsentProcess
    [name order runnable-fn run-fn]
  IConsentProcess
  (runnable? [consent-process arg-map]
    ((:runnable-fn consent-process) arg-map))
  (run [consent-process arg-map]
    ((:run-fn consent-process) arg-map)))

(def default-processes
  (atom []))

(def custom-processes
  (atom []))

(defn build-default-processes
  []
  (reset! default-processes
          (vec (map map->ConsentProcess dp/default-processes))))

(defn build-custom-processes
  []
  (reset! custom-processes
          (vec (map map->ConsentProcess cp/custom-processes))))

(defn build-processes
  []
  (do
    (build-default-processes)
    (build-custom-processes)))

(defn find-custom-process
  [process-name params]
  (first (filter #(and (= process-name (:name %)) (runnable? % params))
                 @custom-processes)))

(defn find-default-process
  [process-name params]
  (first (filter #(and (= process-name (:name %)) (runnable? % params))
                 @default-processes)))
