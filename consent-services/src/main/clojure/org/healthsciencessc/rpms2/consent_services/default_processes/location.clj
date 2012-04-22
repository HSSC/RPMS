(ns org.healthsciencessc.rpms2.consent-services.default-processes.location
  (:use [clojure.data.json :only (json-str pprint-json)]
        [org.healthsciencessc.rpms2.consent-services.domain-utils :only (admin? super-admin? some-kind-of-admin? forbidden-fn)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def loc-processes
  [{:name "get-security-locations"
    :runnable-fn (fn [{{user :current-user} :session}]
                   (some-kind-of-admin? user))
    :run-fn (fn [params]
              (let [user (get-in params [:session :current-user])]
                (cond
                 (super-admin? user)
                 (json-str (data/find-all "location"))
                 (admin? user)
                 (json-str (data/find-siblings (:id user) "user" "organization" "location")))))
    :run-if-false forbidden-fn}

   {:name "get-security-location"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         org-id (get-in user [:organization :id])
                         loc-id (get-in params [:query-params :location])]
                     (or (super-admin? user)
                         (and (admin? user)
                              (data/belongs-to? "location" loc-id "organization" org-id)))))
    :run-fn (fn [params]
              (let [location-id (get-in params [:query-params :location])]
                (json-str (data/find-record "location" location-id))))
    :run-if-false forbidden-fn}

   {:name "put-security-location"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         loc-org-id (get-in params [:body-params :organization :id])]
                     (or (super-admin? user)
                         (and (admin? user)
                              (= user-org-id loc-org-id)))))
    :run-fn (fn [params]
              (let [loc (:body-params params)]
                (json-str (data/create "location" loc))))
    :run-if-false forbidden-fn}

   {:name "post-security-location"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         org-id (get-in user [:organization :id])
                         loc-id (get-in params [:query-params :location])]
                     (or (super-admin? user)
                         (and (admin? user)
                              (data/belongs-to? "location" loc-id "organization" org-id)))))
    :run-fn (fn [params]
              (let [loc-id (get-in params [:query-params :location])
                    loc-data (:body-params params)]
                (json-str (data/update "location" loc-id loc-data))))
    :run-if-false forbidden-fn}

   {:name "delete-security-location"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         loc-id (get-in params [:query-params :location])]
                     (or (super-admin? user)
                         (and (admin? user)
                              (data/belongs-to? "location" loc-id "organization" user-org-id)))))
    :run-fn (fn [params]
              (let [loc-id (get-in params [:query-params :location])]
                (json-str (data/delete "location" loc-id))))
    :run-if-false forbidden-fn}])

(process/register-processes (map #(DefaultProcess/create %) loc-processes))