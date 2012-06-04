(ns org.healthsciencessc.rpms2.consent-services.default-processes.endorsement
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils :only (forbidden-fn)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-domain.roles :as role])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def endorsement-processes
  [{:name "get-library-endorsements"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])]
                     (or (role/consent-collector? user) (role/protocol-designer? user))))
    :run-fn (fn [params]
              (let [user (get-in params [:session :current-user])
                    user-org-id (get-in user [:organization :id])]
                (data/find-children "organization" user-org-id "endorsement")))
    :run-if-false forbidden-fn}

   {:name "get-library-endorsement"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         endorsement-id (get-in params [:query-params :endorsement])]
                     (and (or (role/consent-collector? user) (role/protocol-designer? user))
                          (data/belongs-to? "endorsement" endorsement-id "organization" user-org-id))))
    :run-fn (fn [params]
              (let [endorsement-id (get-in params [:query-params :endorsement])]
                (data/find-record "endorsement" endorsement-id)))
    :run-if-false forbidden-fn}

   {:name "put-library-endorsement"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         item-org-id (get-in params [:body-params :organization :id])]
                     (and (role/protocol-designer? user)
                          (= user-org-id item-org-id))))
    :run-fn (fn [params]
              (let [endorsement (:body-params params)]
                (data/create "endorsement" endorsement)))
    :run-if-false forbidden-fn}

   {:name "post-library-endorsement"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         item-org-id (get-in params [:body-params :organization :id])]
                     (and (role/protocol-designer? user)
                          (= user-org-id item-org-id))))
    :run-fn (fn [params]
              (let [endorsement-id (get-in params [:query-params :endorsement])
                    endorsement (:body-params params)]
                (data/update "endorsement" endorsement-id endorsement)))
    :run-if-false forbidden-fn}

   {:name "delete-library-endorsement"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         item-org-id (get-in params [:body-params :organization :id])]
                     (and (role/protocol-designer? user)
                          (= user-org-id item-org-id))))
    :run-fn (fn [params]
              (let [endorsement-id (get-in params [:query-params :endorsement])]
                (data/delete "endorsement" endorsement-id)))
    :run-if-false forbidden-fn}])

(process/register-processes (map #(DefaultProcess/create %) endorsement-processes))