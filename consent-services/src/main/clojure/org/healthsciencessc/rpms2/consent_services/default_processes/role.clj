(ns org.healthsciencessc.rpms2.consent-services.default-processes.role
  (:use [clojure.data.json :only (json-str pprint-json)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def role-processes
  [{:name "get-security-roles"
    :runnable-fn (fn [params] true)
    :run-fn (fn [params]
              (json-str (data/find-all "role")))}

   {:name "get-security-role"
    :runnable-fn (fn [params] true)
    :run-fn (fn [params]
              (let [role-id (Integer/parseInt (-> params :query-params :role))]
                (json-str (data/find-record "role" role-id))))}

   {:name "put-security-role"
    :runnable-fn (fn [params] true)
    :run-fn (fn [params]
              (let [role (:body-params params)]
                (json-str (data/create "role" role))))}
   
   {:name "post-security-role"
    :runnable-fn (fn [params] true)
    :run-fn (fn [params]
              (let [role-id (Integer/parseInt (get-in params [:query-params :role]))
                    role (-> params :body-params)]
                (json-str (data/update "role" role-id role))))}])

(process/register-processes (map #(DefaultProcess/create %) role-processes))