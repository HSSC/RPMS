(ns org.healthsciencessc.rpms2.consent-services.default-processes.user
  (:use [clojure.data.json :only (json-str pprint-json)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-services.auth :as auth])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def user-processes
  [{:name "authenticate"
   :runnable-fn (fn [params] true)
   :run-fn (fn [{:keys [username password]}]
             (if-let [user-node (first (filter #(= username (get-in % [:data :username])) (data/find-all-instance-nodes "user")))]
               (if (and user-node (auth/good-password? password (get-in user-node [:data :password])))
                 (data/node->record user-node "user"))))}

  {:name "get-security-authenticate"
   :runnable-fn (fn [params] true)
   :run-fn (fn [params]
             (json-str (:current-user (:session params))))}

  {:name "put-security-user"
   :runnable-fn (fn [params] true)
   :run-fn (fn [params]
             (let [user-data (:body-params params)
                   unhashed-pwd (:password user-data)
                   user (assoc user-data :password (auth/hash-password unhashed-pwd))]
               (json-str (data/create "user" user))))}

  {:name "get-security-users"
   :runnable-fn (fn [params] true)
   :run-fn (fn [params]
             (json-str (data/find-all "user")))}

  {:name "get-security-user"
   :runnable-fn (fn [params] true)
   :run-fn (fn [params]
             (let [user-id (Integer/parseInt (-> params :query-params :user))]
               (json-str (data/find-record "user" user-id))))}

  ;; curl -i -X POST -H "Content-type: application/json" -d "{\"username\" : \"foobar\"}" http://localhost:3000/security/user?user=<ID>
  {:name "post-security-user"
   :runnable-fn (fn [params] true)
   :run-fn (fn [params]
             (let [user-id (Integer/parseInt (get-in params [:query-params :user]))
                   user-data (:body-params params)]
               (json-str (data/update "user" user-id user-data))))}])

(process/register-processes (map #(DefaultProcess/create %) user-processes))