(ns org.healthsciencessc.rpms2.consent-services.default-processes.protocol-version
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils :only (forbidden-fn)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-domain.lookup :as lookup]
            [org.healthsciencessc.rpms2.consent-domain.roles :as role]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
            [org.healthsciencessc.rpms2.consent-domain.runnable :as runnable]
            [org.healthsciencessc.rpms2.consent-services.utils :as utils]
            [org.healthsciencessc.rpms2.consent-services.default-processes.protocol :as protocol])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(defn- assign-language
  [ctx]
  (let [language-id (get-in ctx [:query-params :language])
        protocol-version-id (get-in ctx [:query-params :protocol-version])]
    (data/relate-records types/protocol-version protocol-version-id types/language language-id)))

(defn- assign-endorsement
  [ctx]
  (let [endorsement-id (get-in ctx [:query-params :endorsement])
        protocol-version-id (get-in ctx [:query-params :protocol-version])]
    (data/relate-records types/protocol-version protocol-version-id types/endorsement endorsement-id)))

(defn- assign-meta-item
  [ctx]
  (let [meta-item-id (get-in ctx [:query-params :meta-item])
        protocol-version-id (get-in ctx [:query-params :protocol-version])]
    (data/relate-records types/protocol-version protocol-version-id types/meta-item meta-item-id)))

(defn- assign-policy
  [ctx]
  (let [policy-id (get-in ctx [:query-params :policy])
        protocol-version-id (get-in ctx [:query-params :protocol-version])]
    (data/relate-records types/protocol-version protocol-version-id types/policy policy-id)))

(defn- remove-language
  [ctx]
  (let [language-id (get-in ctx [:query-params :language])
        protocol-version-id (get-in ctx [:query-params :protocol-version])]
    (data/unrelate-records types/protocol-version protocol-version-id types/language language-id)))

(defn- remove-endorsement
  [ctx]
  (let [endorsement-id (get-in ctx [:query-params :endorsement])
        protocol-version-id (get-in ctx [:query-params :protocol-version])]
    (data/unrelate-records types/protocol-version protocol-version-id types/endorsement endorsement-id)))

(defn- remove-meta-item
  [ctx]
  (let [meta-item-id (get-in ctx [:query-params :meta-item])
        protocol-version-id (get-in ctx [:query-params :protocol-version])]
    (data/unrelate-records types/protocol-version protocol-version-id types/meta-item meta-item-id)))

(defn- remove-policy
  [ctx]
  (let [policy-id (get-in ctx [:query-params :policy])
        protocol-version-id (get-in ctx [:query-params :protocol-version])]
    (data/unrelate-records types/protocol-version protocol-version-id types/policy policy-id)))

(defn auth-designer-for-protocol
  [ctx]
  (let [protocol-version-id (get-in ctx [:query-params :protocol-version])
        protocol-version (data/find-record types/protocol-version protocol-version-id)
        protocol-id (get-in protocol-version [:protocol :id])
        user (get-in ctx [:session :current-user])]
    (if (protocol/user-is-designer-for-protocol user protocol-id)
      protocol-version 
      false)))

(defn auth-designer-for-protocol-draft
  [ctx]
  (let [protocol-version (auth-designer-for-protocol ctx)]
    (and protocol-version (types/draft? protocol-version))))


(defn auth-designer-for-protocol-submitted
  [ctx]
  (let [protocol-version (auth-designer-for-protocol ctx)]
    (and protocol-version (types/submitted? protocol-version))))


(defn auth-designer-for-protocol-published
  [ctx]
  (let [protocol-version (auth-designer-for-protocol ctx)]
    (and protocol-version (types/published? protocol-version))))

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
    :runnable-fn auth-designer-for-protocol
    :run-fn (fn [params]
              (let [protocol-version-id (get-in params [:query-params :protocol-version])]
                (data/find-record types/protocol-version protocol-version-id)))
    :run-if-false forbidden-fn}

   {:name "put-protocol-version"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         protocol-id (get-in params [:body-params :protocol :id])]
                     (protocol/user-is-designer-for-protocol user protocol-id)))
    :run-fn (fn [params]
              (let [protocol-version (:body-params params)]
                (data/create types/protocol-version (assoc protocol-version :status types/status-draft))))
    :run-if-false forbidden-fn}

   {:name "post-protocol-version"
    :runnable-fn auth-designer-for-protocol-draft
    :run-fn (fn [params]
              (let [protocol-version-id (get-in params [:query-params :protocol-version])
                    protocol-version (:body-params params)]
                (data/update types/protocol-version protocol-version-id protocol-version)))
    :run-if-false forbidden-fn}

   {:name "delete-protocol-version"
    :runnable-fn auth-designer-for-protocol-draft
    :run-fn (fn [params]
              (let [protocol-version-id (get-in params [:query-params :protocol-version])]
                (data/delete types/protocol-version protocol-version-id)))
    :run-if-false forbidden-fn}

   {:name "post-protocol-publish"
    :runnable-fn auth-designer-for-protocol-submitted
    :run-fn (fn [params]
              (let [protocol-version-id (get-in params [:query-params :protocol-version])
                    protocol-version (data/find-record "protocol-version" protocol-version-id)
                    protocol-id (get-in protocol-version [:protocol :id])
                    versions (data/find-children "protocol" protocol-id "protocol-version")
                    published-versions (filter types/published? versions)]
                (doseq [published-version published-versions]
                  (data/update types/protocol-version (:id published-version) {:status types/status-retired}))
                (data/update types/protocol-version protocol-version-id (assoc protocol-version :status types/status-published))))
    :run-if-false forbidden-fn}

   {:name "post-protocol-retire"
    :runnable-fn auth-designer-for-protocol-published
    :run-fn (fn [params]
              (let [protocol-version-id (get-in params [:query-params :protocol-version])
                    protocol-version (data/find-record types/protocol-version protocol-version-id)]
                (data/update types/protocol-version protocol-version-id (assoc protocol-version :status types/status-retired))))
    :run-if-false forbidden-fn}

   {:name "post-protocol-draft"
    :runnable-fn auth-designer-for-protocol-submitted
    :run-fn (fn [params]
              (let [protocol-version-id (get-in params [:query-params :protocol-version])
                    protocol-version (data/find-record types/protocol-version protocol-version-id)]
                (data/update types/protocol-version protocol-version-id (assoc protocol-version :status types/status-draft))))
    :run-if-false forbidden-fn}
   
   {:name "put-protocol-version-language"
    :runnable-fn auth-designer-for-protocol-draft
    :run-fn assign-language
    :run-if-false forbidden-fn}
   
   {:name "put-protocol-version-endorsement"
    :runnable-fn auth-designer-for-protocol-draft
    :run-fn assign-endorsement
    :run-if-false forbidden-fn}
   
   {:name "put-protocol-version-meta-item"
    :runnable-fn auth-designer-for-protocol-draft
    :run-fn assign-meta-item
    :run-if-false forbidden-fn}
   
   {:name "put-protocol-version-policy"
    :runnable-fn auth-designer-for-protocol-draft
    :run-fn assign-policy
    :run-if-false forbidden-fn}
   
   {:name "delete-protocol-version-language"
    :runnable-fn auth-designer-for-protocol-draft
    :run-fn remove-language
    :run-if-false forbidden-fn}
   
   {:name "delete-protocol-version-endorsement"
    :runnable-fn auth-designer-for-protocol-draft
    :run-fn remove-endorsement
    :run-if-false forbidden-fn}
   
   {:name "delete-protocol-version-meta-item"
    :runnable-fn auth-designer-for-protocol-draft
    :run-fn remove-meta-item
    :run-if-false forbidden-fn}
   
   {:name "delete-protocol-version-policy"
    :runnable-fn auth-designer-for-protocol-draft
    :run-fn remove-policy
    :run-if-false forbidden-fn}
   
   {:name "get-protocol-versions-published"
    :runnable-fn (runnable/gen-collector-location-check utils/current-user lookup/get-location-in-query)
    :run-fn (fn [params]
              (let [loc (get-in params [:query-params :location])
                    protocols (data/find-children types/location loc types/protocol)]
                (flatten (for [p protocols]
                  (filter types/published? (data/find-children types/protocol (:id p) types/protocol-version))))))
    :run-if-false forbidden-fn}
   
   {:name "get-protocol-versions-published-meta"
    :runnable-fn (runnable/gen-collector-check utils/current-user)
    :run-fn (fn [params]
              (let [ids (get-in params [:query-params :protocol-version])]
                (distinct (if (coll? ids)
                            (map #(data/find-related-records "protocol-version" % (list "meta-item")) ids)
                            (data/find-related-records "protocol-version" ids (list "meta-item"))))))
    :run-if-false forbidden-fn}])

(process/register-processes (map #(DefaultProcess/create %) protocol-version-processes))