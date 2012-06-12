(ns org.healthsciencessc.rpms2.consent-services.default-processes.encounter
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils :only (forbidden-fn)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-domain.roles :as roles]
            [org.healthsciencessc.rpms2.consent-services.data :as data])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def encounter-roles
  [roles/admin?
   roles/superadmin?
   roles/consent-manager?
   roles/consent-collector?
   roles/system?])

(defn- allowed?
  [u & constraints]
  (apply (some-fn encounter-roles) u constraints))

(def encounter-processes
  [{:name "get-consent-encounters"
    :runnable-fn (fn [{{user :current-user} :session}]
                   (or (roles/admin? user)
                       (roles/superadmin? user)
                       (roles/consent-manager? user)
                       (roles/system? user)))
    :run-fn (fn [params]
              (let [user (get-in params [:session :current-user])
                    user-org-id (get-in user [:organization :id])]
                (cond
                 (roles/superadmin? user)
                 (data/find-all "encounter")
                 (allowed? user)
                 (data/find-children "organization" user-org-id "encounter"))))
    :run-if-false forbidden-fn}

   {:name "get-consent-encounter"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         org-id (get-in user [:organization :id])
                         enc-id (get-in params [:query-params :encounter])]
                     (and (allowed? user)
                          (data/belongs-to? "encounter" enc-id "organization" org-id))))
    :run-fn (fn [params]
              (let [encounter-id (get-in params [:query-params :encounter])]
                (data/find-record "encounter" encounter-id)))
    :run-if-false forbidden-fn}

   {:name "put-consent-encounter"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])]
                     (allowed? user)))
    :run-fn (fn [params]
              (let [enc (:body-params params)]
                (data/create "encounter" (assoc enc :organization
                                           (get-in params :user :organization)))))
    :run-if-false forbidden-fn}

   {:name "post-consent-encounter"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         org-id (get-in user [:organization :id])
                         enc-id (get-in params [:query-params :encounter])]
                     (and (allowed? user)
                          (data/belongs-to? "encounter" enc-id "organization" org-id false))))
    :run-fn (fn [params]
              (let [enc-id (get-in params [:query-params :encounter])
                    enc-data (:body-params params)]
                (data/update "encounter" enc-id enc-data)))
    :run-if-false forbidden-fn}

   {:name "delete-consent-encounter"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         org-id (get-in user [:organization :id])
                         enc-id (get-in params [:query-params :encounter])]
                     (and (allowed? user)
                          (data/belongs-to? "encounter" enc-id "organization" org-id false))))
    :run-fn (fn [params]
              (let [enc-id (get-in params [:query-params :encounter])]
                (data/delete "encounter" enc-id)))
    :run-if-false forbidden-fn}])

(process/register-processes (map #(DefaultProcess/create %) encounter-processes))
