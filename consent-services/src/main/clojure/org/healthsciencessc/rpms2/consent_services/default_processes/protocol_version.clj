(ns org.healthsciencessc.rpms2.consent-services.default-processes.protocol-version
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils :only (forbidden-fn current-user)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-domain.roles :as role]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
            [org.healthsciencessc.rpms2.consent-domain.runnable :as runnable]
            [org.healthsciencessc.rpms2.consent-services.default-processes.protocol :as protocol])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def protocol-version-processes
  [{:name "get-protocol-versions"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         protocol-id (get-in params [:query-params :protocol])]
                     (protocol/user-is-designer-for-protocol user protocol-id)))
    :run-fn (fn [params]
              (let [protocol (get-in params [:query-params :protocol])]
                (data/find-children types/protocol protocol types/protocol-version)))
    :run-if-false forbidden-fn}

   {:name "get-protocol-version"
    :runnable-fn (fn [params]
                   (let [protocol-version-id (get-in params [:query-params :version])
                         protocol-version (data/find-record types/protocol-version protocol-version-id)
                         protocol-id (get-in protocol-version [:protocol :id])
                         user (get-in params [:session :current-user])]
                     (protocol/user-is-designer-for-protocol user protocol-id)))
    :run-fn (fn [params]
              (let [protocol-version-id (get-in params [:query-params :version])]
                (data/find-record types/protocol-version protocol-version-id)))
    :run-if-false forbidden-fn}

   {:name "put-protocol-version"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         protocol-version (:body-params params)
                         protocol-id (get-in protocol-version [:protocol :id])]
                     (protocol/user-is-designer-for-protocol user protocol-id)))
    :run-fn (fn [params]
              (let [protocol-version (:body-params params)]
                (data/create types/protocol-version (assoc protocol-version :status types/status-draft))))
    :run-if-false forbidden-fn}

   {:name "post-protocol-version"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         protocol-version-id (get-in params [:query-params :version])
                         protocol-version (data/find-record types/protocol-version protocol-version-id)
                         protocol-id (get-in protocol-version [:protocol :id])]
                     (and (protocol/user-is-designer-for-protocol user protocol-id)
                          (types/draft? protocol-version))))
    :run-fn (fn [params]
              (let [protocol-version-id (get-in params [:query-params :version])
                    protocol-version (:body-params params)]
                (data/update types/protocol-version protocol-version-id protocol-version)))
    :run-if-false forbidden-fn}

   {:name "delete-protocol-version"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         protocol-version-id (get-in params [:query-params :version])
                         protocol-version (data/find-record types/protocol-version protocol-version-id)
                         protocol-id (get-in protocol-version [:protocol :id])]
                     (and (protocol/user-is-designer-for-protocol user protocol-id)
                          (types/draft? protocol-version))))
    :run-fn (fn [params]
              (let [protocol-version-id (get-in params [:query-params :version])]
                (data/delete types/protocol-version protocol-version-id)))
    :run-if-false forbidden-fn}

   {:name "post-protocol-publish"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         protocol-version-id (get-in params [:query-params :version])
                         protocol-version (data/find-record types/protocol-version protocol-version-id)
                         protocol-id (get-in protocol-version [:protocol :id])]
                     (and (protocol/user-is-designer-for-protocol user protocol-id)
                          (types/draft? protocol-version))))
    :run-fn (fn [params]
              (let [protocol-version-id (get-in params [:query-params :version])
                    protocol-version (data/find-record "protocol-version" protocol-version-id)
                    protocol-id (get-in protocol-version [:protocol :id])
                    versions (data/find-children "protocol" protocol-id "protocol-version")
                    published-versions (filter types/published? versions)]
                (doseq [published-version published-versions]
                  (data/update types/protocol-version (:id published-version) {:status types/status-retired}))
                (data/update types/protocol-version protocol-version-id (assoc protocol-version :status types/status-published))))
    :run-if-false forbidden-fn}

   {:name "post-protocol-retire"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         protocol-version-id (get-in params [:query-params :version])
                         protocol-version (data/find-record types/protocol-version protocol-version-id)
                         protocol-id (get-in protocol-version [:protocol :id])]
                     (and (protocol/user-is-designer-for-protocol user protocol-id)
                          (types/published? protocol-version))))
    :run-fn (fn [params]
              (let [protocol-version-id (get-in params [:query-params :version])
                    protocol-version (data/find-record types/protocol-version protocol-version-id)]
                (data/update types/protocol-version protocol-version-id (assoc protocol-version :status types/status-retired))))
    :run-if-false forbidden-fn}

   {:name "get-protocol-versions-published"
    :runnable-fn (runnable/gen-collector-location-check current-user [:query-params :location])
    :run-fn (fn [params]
              (let [loc (get-in params [:query-params :location])]
                (filter (partial = types/status-published) (data/find-children types/location loc types/protocol))))
    :run-if-false forbidden-fn}
   
   {:name "get-protocol-versions-published-meta"
    :runnable-fn (runnable/gen-collector-check current-user)
    :run-fn (fn [params]
              (let [ids (get-in params [:query-params :version])]
                (distinct (if (coll? ids)
                            (map #(data/find-related-records "protocol-version" % (list "meta-item")) ids)
                            (data/find-related-records "protocol-version" ids (list "meta-item"))))))
    :run-if-false forbidden-fn}])

(process/register-processes (map #(DefaultProcess/create %) protocol-version-processes))