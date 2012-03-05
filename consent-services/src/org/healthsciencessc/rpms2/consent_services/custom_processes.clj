(ns org.healthsciencessc.rpms2.consent-services.custom-processes
  (:require [org.healthsciencessc.rpms2.consent-services.process :as process])
  (:import [org.healthsciencessc.rpms2.consent_services.process CustomProcess]))

(def process-defns
  [{:name "get-security-users"
    :order 1
    :runnable-fn (fn [& args] false)
    :run-fn (fn [& args]
              (println "Before 1")
              (println (process/run-default "get-security-users" args))
              (println "After 1"))}
   {:name "get-security-users"
    :order 2
    :runnable-fn (fn [& args] true)
    :run-fn (fn [& args]
              (println "Before 2")
              (println (process/run-default "get-security-users" args))
              (println "After 2"))}])

(process/register-processes (map #(CustomProcess/create %) process-defns))