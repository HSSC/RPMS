(ns org.healthsciencessc.rpms2.consent-services.default-processes.location
  (:use [clojure.data.json :only (json-str pprint-json)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def loc-processes
  [{:name "get-security-locations"
    :runnable-fn (fn [params] true)
    :run-fn (fn [params]
              (json-str (data/find-all "location")))}

   {:name "get-security-location"
    :runnable-fn (fn [params] true)
    :run-fn (fn [params]
              (let [location-id (Integer/parseInt (-> params :query-params :location))]
                (json-str (data/find-record "location" location-id))))}
   
   {:name "put-security-location"
    :runnable-fn (fn [params] true)
    :run-fn (fn [params]
              (let [loc (:body-params params)]
                (json-str (data/create "location" loc))))}

   {:name "post-security-location"
    :runnable-fn (fn [params] true)
    :run-fn (fn [params]
              (let [loc-id (Integer/parseInt (get-in params [:query-params :location]))
                    loc-data (:body-params params)]
                (json-str (data/update "location" loc-id loc-data))))}])

(process/register-processes (map #(DefaultProcess/create %) loc-processes))