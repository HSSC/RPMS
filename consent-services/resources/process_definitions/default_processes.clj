(ns org.healthsciencessc.rpms2.consent-services.default-processes
  (:use [clojure.data.json :only (json-str)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-services.auth :as auth])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def process-defns
  [
   ;; curl -i -X PUT -H "Content-type: application/json" -d "{\"name\" : \"MUSC FOOBAR\"}" http://localhost:3000/organization
   {:name "put-organization"
    :runnable-fn (fn [params] true)
    :run-fn (fn [params]
              (let [org (:body-params params)]
                (json-str (data/create "organization" org))))}

   ;; curl -i -X GET -H "Content-type: application/json" http://localhost:3000/organizations
   {:name "get-organizations"
    :runnable-fn (fn [params] true)
    :run-fn (fn [params]
              (json-str (data/find-all "organization")))}

   ;; curl -i -X GET -H "Content-type: application/json" http://localhost:3000/organization?organization=<ID>
   {:name "get-organization"
    :runnable-fn (fn [params] true)
    :run-fn (fn [params]
              (let [org-id (Integer/parseInt (-> params :query-params :organization))]
                (json-str (data/find-record "organization" org-id))))}

   ;; curl -i -X POST -H "Content-type: application/json" -d "{\"id\" : <ID> \"name\" : \"MUSC FOOBAR\"}" http://localhost:3000/organization
   {:name "post-organization"
    :runnable-fn (fn [params] true)
    :run-fn (fn [params]
              (let [org (-> params :body-params :organization)]
                (json-str (data/update "organization" org))))}

   {:name "authenticate"
    :runnable-fn (fn [params] true)
    :run-fn (fn [{:keys [username password]}]
              (if-let [user (first (data/find-records-by-attrs (:username username)))]
                (when (= password (auth/hash-password (:password user) (:salt user)))
                  user)))}

   {:name "put-user"
    :runnable-fn (fn [params] true)
    :run-fn (fn [params]
              (let [user-data (:body-params params)
                    unhashed-pwd (:password user-data)
                    new-salt (auth/generate-salt)
                    user (assoc user-data :salt new-salt :password (auth/hash-password unhashed-pwd new-salt))]
                (json-str (data/create "user" user))))}

   {:name "get-users"
    :runnable-fn (fn [params] true)
    :run-fn (fn [params]
              (json-str (data/find-all "user")))}
   
   {:name "get-user"
    :runnable-fn (fn [params] true)
    :run-fn (fn [params]
              (let [user-id (Integer/parseInt (-> params :query-params :user))]
                (json-str (data/find-record "user" user-id))))}

   ;; curl -i -X POST -H "Content-type: application/json" -d "{\"id\" : <ID> \"name\" : \"MUSC FOOBAR\"}" http://localhost:3000/organization
   {:name "post-user"
    :runnable-fn (fn [params] true)
    :run-fn (fn [params]
              (let [user (:body-params params)]
                (json-str (data/update "user" user))))}
   
   ])

(process/register-processes (map #(DefaultProcess/create %) process-defns))
