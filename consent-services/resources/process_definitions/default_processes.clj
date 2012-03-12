(ns org.healthsciencessc.rpms2.consent-services.default-processes
  (:use [clojure.data.json :only (json-str)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.domain-api :as domain-api])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def process-defns
  [
   ;; curl -i -X PUT -H "Content-type: application/json" -d
   ;; "{\"organization\" : {\"name\" : \"MUSC FOOBAR\"}}" http://localhost:3000/organization
   {:name "put-organization"
    :runnable-fn (fn [params] true)
    :run-fn (fn [params]
              (let [org (-> params :body-params :organization)]
                (json-str (domain-api/create-organization org))))}

   ;; curl -i -X GET -H "Content-type: application/json" http://localhost:3000/organizations
   {:name "get-organizations"
    :runnable-fn (fn [params] true)
    :run-fn (fn [params]
              (json-str (domain-api/find-all-organizations)))}
   
   ;; curl -i -X GET -H "Content-type: application/json" http://localhost:3000/organization?organization=<ID>
   {:name "get-organization"
    :runnable-fn (fn [params] true)
    :run-fn (fn [params]
              (let [org-id (Integer/parseInt (-> params :query-params :organization))]
                (json-str (domain-api/find-organization org-id))))}

   ;; curl -i -X PUT -H "Content-type: application/json" -d
   ;; "{\"organization\" : {\"id\" : <ID> \"name\" : \"MUSC FOOBAR\"}}" http://localhost:3000/organization
   {:name "post-organization"
    :runnable-fn (fn [params] true)
    :run-fn (fn [params]
              (let [org (-> params :body-params :organization)]
                (json-str (domain-api/update-organization org))))}

   {:name "authenticate"
    :runnable-fn (fn [params] true)
    :run-fn (fn [{:keys [username password]}]
              (println username)
              (if (= username "colin")
                true))}
   ])

(process/register-processes (map #(DefaultProcess/create %) process-defns))
