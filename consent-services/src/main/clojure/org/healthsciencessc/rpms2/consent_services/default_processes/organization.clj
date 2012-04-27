(ns org.healthsciencessc.rpms2.consent-services.default-processes.organization
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils :only (admin? super-admin? some-kind-of-admin? forbidden-fn)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def org-processes
  [;; curl -i -X GET -H "Content-type: application/json" http://localhost:3000/organizations
   {:name "get-security-organizations"
    :runnable-fn (fn [{{user :current-user} :session}]
                   (some-kind-of-admin? user))
    :run-fn (fn [params]
              (let [user (get-in params [:session :current-user])]
                (cond (super-admin? user)
                      (data/find-all "organization")
                      (admin? user)
                      (vector (data/find-record "organization" (get-in user [:organization :id]))))))
    :run-if-false forbidden-fn}

   ;; curl -i -X GET -H "Content-type: application/json" http://localhost:3000/organization?organization=<ID>
   {:name "get-security-organization"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         org-id (get-in params [:query-params :organization])]
                     (or (super-admin? user)
                         (and (admin? user) (= user-org-id org-id)))))
    :run-fn (fn [params]
              (let [org-id (get-in params [:query-params :organization])]
                (data/find-record "organization" org-id)))
    :run-if-false forbidden-fn}

   ;; curl -i -X PUT -H "Content-type: application/json" -d "{\"name\" : \"MUSC FOOBAR\"}" http://localhost:3000/organization
   {:name "put-security-organization"
    :runnable-fn (fn [{{user :current-user} :session}] (super-admin? user))
    :run-fn (fn [params]
              (let [org (:body-params params)]
                (data/create "organization" org)))
    :run-if-false forbidden-fn}

   ;; curl -i -X POST -H "Content-type: application/json" -d "{\"name\" : \"MUSC BAZ\"}" http://localhost:3000/organization?organization=<ID>
   {:name "post-security-organization"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         org-id (get-in params [:query-params :organization])]
                     (or (super-admin? user)
                         (and (admin? user) (= user-org-id org-id)))))
    :run-fn (fn [params]
              (let [org-id (get-in params [:query-params :organization])
                    org (-> params :body-params)]
                (data/update "organization" org-id org)))
    :run-if-false forbidden-fn}

   {:name "delete-security-organization"
    :runnable-fn (fn [{{user :current-user} :session}] (super-admin? user))
    :run-fn (fn [params]
              (let [org-id (get-in params [:query-params :organization])]
                (data/delete "organization" org-id)))
    :run-if-false forbidden-fn}])

(process/register-processes (map #(DefaultProcess/create %) org-processes))