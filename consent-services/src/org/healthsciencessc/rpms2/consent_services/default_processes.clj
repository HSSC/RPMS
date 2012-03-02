(ns org.healthsciencessc.rpms2.consent-services.default-processes
  (:use [clojure.data.json :only (json-str)])
  (:require [org.healthsciencessc.rpms2.consent-services.data :as data]))

(def default-processes
  [{:name "get-security-users"
    :runnable-fn (fn [& args] true)
    :run-fn (fn [& args]
              (json-str (data/get-all-users)))}])