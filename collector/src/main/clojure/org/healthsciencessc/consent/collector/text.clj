(ns org.healthsciencessc.consent.collector.text
  (:require [org.healthsciencessc.consent.domain.tenancy :as tenancy]
            [org.healthsciencessc.consent.collector.state :as state])
  (:use     [pliant.process :only [defprocess]]))

(def ^:private bundle (java.util.ResourceBundle/getBundle "consent/collect"))

;; Text Retrieval Functions
(defprocess text
  ([key] (text key {}))
  ([key options]
    (let [prop (name key)]
      (or (.getString bundle prop) prop))))


(defn format-text
  [key {:keys [args] :as options}]
  (let [value (text key options)]
    (apply format value args)))


(defn location-text
  ([key] (location-text key {}))
  ([key {:keys [location organization] :as options}]
    (let [loc (or location {})
          org (or organization (state/get-organization))
          loc-text (tenancy/label-for-location loc org)]
      (format-text key (assoc options :args (cons loc-text (:args options)))))))


(defn protocol-text
  ([key] (protocol-text key {}))
  ([key {:keys [location organization] :as options}]
    (let [loc (or location (state/get-location))
          org (or organization (state/get-organization))
          prot-text (tenancy/label-for-protocol loc org)]
      (format-text key (assoc options :args (cons prot-text (:args options)))))))


(defn consenter-text
  ([key] (consenter-text key {}))
  ([key {:keys [location organization] :as options}]
    (let [loc (or location (state/get-location))
          org (or organization (state/get-organization))
          cons-text (tenancy/label-for-consenter loc org)]
      (format-text key (assoc options :args (cons cons-text (:args options)))))))


(defn encounter-text
  ([key] (encounter-text key {}))
  ([key {:keys [location organization] :as options}]
    (let [loc (or location (state/get-location))
          org (or organization (state/get-organization))
          enc-text (tenancy/label-for-encounter loc org)]
      (format-text key (assoc options :args (cons enc-text (:args options)))))))

