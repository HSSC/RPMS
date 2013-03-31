(ns org.healthsciencessc.consent.services.process.protocol-version-published
  (:use     [pliant.process :only [defprocess as-method]])
  (:require [org.healthsciencessc.consent.services.data :as data]
            [org.healthsciencessc.consent.services.respond :as respond]
            [org.healthsciencessc.consent.services.session :as session]
            [org.healthsciencessc.consent.services.vouch :as vouch]
            [org.healthsciencessc.consent.domain.roles :as roles]
            [org.healthsciencessc.consent.domain.types :as types]
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint]))



(def reformatted-types
  [:policies :endorsements :meta-items :form])

(defn get-lang-value
  [text-coll lang-map]
  (let [lang-id (:id (:lang lang-map))
        default-lang-id (:id (:default-lang lang-map))]
    (:value (or (first (filter (fn [text] (= lang-id (get-in text [:language :id]))) text-coll))
                (first (filter (fn [text] (= default-lang-id (get-in text [:language :id]))) text-coll))))))

(defn map-by-id
  [coll value-fn]
  (into {}
        (for [elem coll]
          [(:id elem) (value-fn elem)])))

(defmulti reformat-type
  (fn [type data lang-map]
    type))

(defmethod reformat-type :policies
  [type policies lang-map]
  (map-by-id policies
             (fn [policy]
               {:title (get-lang-value (:titles policy) lang-map)
                :text (get-lang-value (:texts policy) lang-map)})))

(defmethod reformat-type :meta-items
  [type meta-items lang-map]
  (map-by-id meta-items
             (fn [meta-item]
               {:label (get-lang-value (:labels meta-item) lang-map)})))

(defmethod reformat-type :endorsements
  [type endorsements lang-map]
  (map-by-id endorsements
             (fn [endorsement]
               {:label (get-lang-value (:labels endorsement) lang-map)
                :endorsement-type (:endorsement-type endorsement)})))

(defmethod reformat-type :form
  [type form lang-map]
  (assoc (select-keys form [:contains :collect-start :review-start :witness-signatures])
    :title (get-lang-value (:titles form) lang-map)))

(defn reformat-version-data
  [protocol-version lang-map]
  (assoc (into {}
               (for [type reformatted-types]
                 [type (reformat-type type (type protocol-version) lang-map)]))
         :id (:id protocol-version)))


(defprocess get-published-protocol-versions
  [ctx]
  (let [location (vouch/collects-or-designs-location ctx)]
    (if location
      (let [protocols (data/find-children types/location (:id location) types/protocol)]
        (flatten (for [p protocols]
                   (filter types/published? (data/find-children types/protocol (:id p) types/protocol-version)))))
      (respond/forbidden))))

(as-method get-published-protocol-versions endpoint/endpoints "get-protocol-versions-published")


(defprocess get-published-protocol-versions-meta
  [ctx]
  (let [protocol-versions (vouch/collects-or-designs-protocol-versions ctx)]
    (if protocol-versions
      (let [ids (map :id protocol-versions)]
        (distinct (apply concat (map #(data/find-related-records types/protocol-version % (list types/meta-item)) ids))))
      (respond/forbidden))))

(as-method get-published-protocol-versions-meta endpoint/endpoints "get-protocol-versions-published-meta")


(defprocess get-published-protocol-versions-form
  [ctx]
  (let [protocol-versions (vouch/collects-or-designs-protocol-versions ctx)]
    (if protocol-versions
      (let [default-lang (:language (session/current-org ctx))
            lang-id (get-in ctx [:query-params :language])
            requested-lang (if lang-id (data/find-record types/language lang-id) default-lang)]
        (map #(reformat-version-data % {:lang requested-lang :default-lang default-lang}) protocol-versions))
      (respond/forbidden))))

(as-method get-published-protocol-versions-form endpoint/endpoints "get-protocol-versions-published-form")
