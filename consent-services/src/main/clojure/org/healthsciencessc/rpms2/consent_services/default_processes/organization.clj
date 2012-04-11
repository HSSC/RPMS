(ns org.healthsciencessc.rpms2.consent-services.default-processes.organization
  (:use [clojure.data.json :only (json-str pprint-json)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def org-processes
  [;; curl -i -X GET -H "Content-type: application/json" http://localhost:3000/organizations
   {:name "get-security-organizations"
    :runnable-fn (fn [params] true)
    :run-fn (fn [params]
              (json-str (data/find-all "organization")))}

   ;; curl -i -X GET -H "Content-type: application/json" http://localhost:3000/organization?organization=<ID>
   {:name "get-security-organization"
    :runnable-fn (fn [params] true)
    :run-fn (fn [params]
              (let [org-id (Integer/parseInt (-> params :query-params :organization))]
                (json-str (data/find-record "organization" org-id))))}

   ;; curl -i -X PUT -H "Content-type: application/json" -d "{\"name\" : \"MUSC FOOBAR\"}" http://localhost:3000/organization
   {:name "put-security-organization"
    :runnable-fn (fn [params] true)
    :run-fn (fn [params]
              (let [org (:body-params params)]
                (json-str (data/create "organization" org))))}
   
   ;; curl -i -X POST -H "Content-type: application/json" -d "{\"name\" : \"MUSC BAZ\"}" http://localhost:3000/organization?organization=<ID>
   {:name "post-security-organization"
    :runnable-fn (fn [params] true)
    :run-fn (fn [params]
              (let [org-id (Integer/parseInt (get-in params [:query-params :organization]))
                    org (-> params :body-params)]
                (json-str (data/update "organization" org-id org))))}])

(process/register-processes (map #(DefaultProcess/create %) org-processes))