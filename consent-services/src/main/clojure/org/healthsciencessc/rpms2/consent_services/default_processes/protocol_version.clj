(ns org.healthsciencessc.rpms2.consent-services.default-processes.protocol-version
  (:use     [pliant.process :only [defprocess as-method]])
  (:require [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-services.respond :as respond]
            [org.healthsciencessc.rpms2.consent-services.vouch :as vouch]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint]
            [clojure.walk :as walk]
            [borneo.core :as neo]))


(defn next-version
  [protocol-version]
  (let [protocol (:protocol protocol-version)
        siblings (data/find-children types/protocol (:id protocol) types/protocol-version)]
    (str (inc (count siblings)))))

(defn nonpublished?
  [protocol-version]
  (or (types/draft? protocol-version) (types/submitted? protocol-version)))

(defn has-nonpublished-siblings?
  [protocol-version]
  (let [protocol (:protocol protocol-version)
        siblings (data/find-children types/protocol (:id protocol) types/protocol-version)]
    (not (nil? (some nonpublished? siblings)))))

(defn- clean-form
  [protocol-version]
  (let [ids (set (conj 
              (for [lang (:languages protocol-version)] (:id lang)) 
              (get-in protocol-version [:organization :id])))]
    (walk/postwalk 
      (fn [o] (if 
                (map? o) 
                (if (contains? ids (:id o))
                  {:id (:id o)}
                  (dissoc o :id))
                o))
      (:form protocol-version))))

(defn- clean-version
  [protocol-version form]
  (let [version {:version (next-version protocol-version) 
                 :status types/status-draft 
                 :protocol (:protocol protocol-version) 
                 :organization (:organization protocol-version) 
                 :form form}
        record (data/create types/protocol-version version)]
    (doseq [lang (:languages protocol-version)] 
      (data/relate-records types/protocol-version (:id record) types/language (:id lang)))
    (doseq [meta-item (:meta-items protocol-version)] 
      (data/relate-records types/protocol-version (:id record) types/meta-item (:id meta-item)))
    (doseq [endorsment (:endorsements protocol-version)] 
      (data/relate-records types/protocol-version (:id record) types/endorsement (:id endorsment)))
    (doseq [policy (:policies protocol-version)] 
      (data/relate-records types/protocol-version (:id record) types/policy (:id policy)))
    record))



(defprocess get-protocol-versions
  [ctx]
  (let [protocol (vouch/designs-protocol ctx)]
    (if protocol
      (data/find-children types/protocol (:id protocol) types/protocol-version)
      (respond/forbidden))))

(as-method get-protocol-versions endpoint/endpoints "get-protocol-versions")


(defprocess get-protocol-version
  [ctx]
  (let [protocol-version (vouch/designs-protocol-version ctx)]
    (if protocol-version
      protocol-version
      (respond/forbidden))))

(as-method get-protocol-version endpoint/endpoints "get-protocol-version")


(defprocess add-protocol-version
  [ctx]
  (let [protocol (vouch/designs-protocol ctx)]
    (if protocol
      (if (has-nonpublished-siblings? {:protocol protocol})
        (respond/forbidden "Protocol must not have any versions in draft or submitted status in order to create new version.")
        (let [org (:organization protocol)
              data (assoc (:body-params ctx) 
                          :organization org 
                          :protocol protocol 
                          :status types/status-draft)]
          (data/create types/protocol-version data)))
      (respond/forbidden))))

(as-method add-protocol-version endpoint/endpoints "put-protocol-version")


(defprocess update-protocol-version
  [ctx]
  (let [protocol-version (vouch/designs-protocol-version ctx)]
    (if protocol-version
      (if (types/draft? protocol-version)
        (data/update types/protocol-version (:id protocol-version) (:body-params ctx))
        (respond/forbidden "Protocol must must be in draft in order to change."))
      (respond/forbidden))))

(as-method update-protocol-version endpoint/endpoints "post-protocol-version")


(defprocess delete-protocol-version
  [ctx]
  (let [protocol-version (vouch/designs-protocol-version ctx)]
    (if protocol-version
      (if (nonpublished? protocol-version)
        (data/delete types/protocol-version (:id protocol-version))
        (respond/forbidden "Protocol must must be non-published in order to delete."))
      (respond/forbidden))))

(as-method delete-protocol-version endpoint/endpoints "delete-protocol-version")


(defprocess clone-protocol-version
  [ctx]
  (let [protocol-version (vouch/designs-protocol-version ctx)]
    (if protocol-version
      (cond
        (nonpublished? protocol-version)
          (respond/forbidden "Protocol Version must be published to clone.")
        (has-nonpublished-siblings? protocol-version)
          (respond/forbidden "Protocol must not have any versions in draft or submitted status to perform clone.")
        :else
        (let [form (clean-form protocol-version)
              widgets (:contains form)
              naked-form (dissoc form :contains)]
          (neo/with-tx
            (let [version (clean-version protocol-version naked-form)
                  new-form (:form version)]
              (doseq [widget widgets]
                (let [w (data/create-records types/widget (dissoc widget :form))]
                  (data/relate-records types/widget (:id w) types/form (:id new-form))))
              version))))
      (respond/forbidden))))

(as-method clone-protocol-version endpoint/endpoints "post-protocol-version-clone")


(defprocess publish
  [ctx]
  (let [protocol-version (vouch/designs-protocol-version ctx)]
    (if protocol-version
      (if (types/submitted? protocol-version)
        (let [protocol-id (get-in protocol-version [:protocol :id])
              versions (data/find-children types/protocol protocol-id types/protocol-version)
              published-versions (filter types/published? versions)]
          (neo/with-tx
            (doseq [published-version published-versions]
              (data/update types/protocol-version (:id published-version) {:status types/status-retired}))
            (data/update types/protocol-version (:id protocol-version) (assoc protocol-version :status types/status-published))))
        (respond/forbidden "A protocol version must be in a submitted status in order to be published."))
      (respond/forbidden))))

(as-method publish endpoint/endpoints "post-protocol-version-publish")


(defprocess retire
  [ctx]
  (let [protocol-version (vouch/designs-protocol-version ctx)]
    (if protocol-version
      (if (types/published? protocol-version)
        (data/update types/protocol-version (:id protocol-version) {:status types/status-retired})
        (respond/forbidden "Only published versions can be retired."))
      (respond/forbidden))))

(as-method retire endpoint/endpoints "post-protocol-version-retire")


(defprocess draft
  [ctx]
  (let [protocol-version (vouch/designs-protocol-version ctx)]
    (if protocol-version
      (if (types/submitted? protocol-version)
        (data/update types/protocol-version (:id protocol-version) {:status types/status-draft})
        (respond/forbidden "Only submitted versions can be reverted to a draft status."))
      (respond/forbidden))))

(as-method draft endpoint/endpoints "post-protocol-version-draft")


(defprocess submit
  [ctx]
  (let [protocol-version (vouch/designs-protocol-version ctx)]
    (if protocol-version
      (if (types/draft? protocol-version)
        (data/update types/protocol-version (:id protocol-version) {:status types/status-submitted})
        (respond/forbidden "Only submitted versions can be reverted to a draft status."))
      (respond/forbidden))))

(as-method submit endpoint/endpoints "post-protocol-version-submit")


(defprocess assign-language
  [ctx]
  (let [protocol-version (vouch/designs-protocol-version ctx)
        language-id (get-in ctx [:query-params :language])]
    (if protocol-version
      (data/relate-records types/protocol-version (:id protocol-version) types/language language-id)
      (respond/forbidden))))

(as-method assign-language endpoint/endpoints "put-protocol-version-language")


(defprocess assign-endorsement
  [ctx]
  (let [protocol-version (vouch/designs-protocol-version ctx)
        endorsement-id (get-in ctx [:query-params :endorsement])]
    (if protocol-version
      (data/relate-records types/protocol-version (:id protocol-version) types/endorsement endorsement-id)
      (respond/forbidden))))

(as-method assign-endorsement endpoint/endpoints "put-protocol-version-endorsement")


(defprocess assign-meta-item
  [ctx]
  (let [protocol-version (vouch/designs-protocol-version ctx)
        meta-item-id (get-in ctx [:query-params :meta-item])]
    (if protocol-version
      (data/relate-records types/protocol-version (:id protocol-version) types/meta-item meta-item-id)
      (respond/forbidden))))

(as-method assign-meta-item endpoint/endpoints "put-protocol-version-meta-item")


(defprocess assign-policy
  [ctx]
  (let [protocol-version (vouch/designs-protocol-version ctx)
        policy-id (get-in ctx [:query-params :policy])]
    (if protocol-version
      (data/relate-records types/protocol-version (:id protocol-version) types/policy policy-id)
      (respond/forbidden))))

(as-method assign-policy endpoint/endpoints "put-protocol-version-policy")


(defprocess unassign-language
  [ctx]
  (let [protocol-version (vouch/designs-protocol-version ctx)
        language-id (get-in ctx [:query-params :language])]
    (if protocol-version
      (data/unrelate-records types/protocol-version (:id protocol-version) types/language language-id)
      (respond/forbidden))))

(as-method unassign-language endpoint/endpoints "delete-protocol-version-language")


(defprocess unassign-endorsement
  [ctx]
  (let [protocol-version (vouch/designs-protocol-version ctx)
        endorsement-id (get-in ctx [:query-params :endorsement])]
    (if protocol-version
      (data/unrelate-records types/protocol-version (:id protocol-version) types/endorsement endorsement-id)
      (respond/forbidden))))

(as-method unassign-endorsement endpoint/endpoints "delete-protocol-version-endorsement")


(defprocess unassign-meta-item
  [ctx]
  (let [protocol-version (vouch/designs-protocol-version ctx)
        meta-item-id (get-in ctx [:query-params :meta-item])]
    (if protocol-version
      (data/unrelate-records types/protocol-version (:id protocol-version) types/meta-item meta-item-id)
      (respond/forbidden))))

(as-method unassign-meta-item endpoint/endpoints "delete-protocol-version-meta-item")


(defprocess unassign-policy
  [ctx]
  (let [protocol-version (vouch/designs-protocol-version ctx)
        policy-id (get-in ctx [:query-params :policy])]
    (if protocol-version
      (data/unrelate-records types/protocol-version (:id protocol-version) types/policy policy-id)
      (respond/forbidden))))

(as-method unassign-policy endpoint/endpoints "delete-protocol-version-policy")

