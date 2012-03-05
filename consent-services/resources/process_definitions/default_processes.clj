(ns org.healthsciencessc.rpms2.consent-services.default-processes
  (:use [clojure.data.json :only (json-str)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess])
  )

(def process-defns
  [{:name "get-security-users"
    :runnable-fn (fn [& args] true)
    :run-fn (fn [& args]
              (json-str (data/get-all-users)))}])

(process/register-processes (map #(DefaultProcess/create %) process-defns))
