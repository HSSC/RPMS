(ns org.healthsciencessc.rpms2.consent-services.default-processes.protocol-version
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils :only (forbidden-fn)])
  (:require [clojure.walk :as walk]
            [ring.util.response :as rutil]
            [borneo.core :as neo]
            [org.healthsciencessc.rpms2.process-engine.core :as process]
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
  (assoc (select-keys form [:contains :collect-start :review-start])
    :title (get-lang-value (:titles form) lang-map)))

(defn reformat-version-data
  [version-id lang-map]
  (let [protocol-version (data/find-record types/protocol-version version-id)]
    (assoc (into {}
                 (for [type reformatted-types]
                   [type (reformat-type type (type protocol-version) lang-map)]))
      :id version-id)))


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

(defn clone-version
  [ctx]
  (let [protocol-version-id (get-in ctx [:query-params :protocol-version])
        protocol-version (data/find-record types/protocol-version protocol-version-id)]
    (cond
      (nonpublished? protocol-version)
        (rutil/status 
          (rutil/response 
            {:message "Protocol Version must be published to clone."}) 403)
      (has-nonpublished-siblings? protocol-version)
        (rutil/status 
          (rutil/response 
            {:message "Protocol must not have any versions in draft or submitted status to perform clone."}) 403)
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
              version))))))

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

   {:name "get-protocol-versions-published-form"
    :runnable-fn (fn [params]
                   (let [q-params (get-in params [:query-params :protocol-version])
                         protocol-version-ids (if (coll? q-params) q-params (list q-params))
                         protocol-versions (map (partial data/find-record types/protocol-version) protocol-version-ids)
                         locations (map #(get-in % [:protocol :location]) protocol-versions)]
                     (or (every? #(runnable/can-design-protocol-version (utils/current-user params) %) protocol-versions)
                         (every? #(runnable/can-collect-location (utils/current-user params) %) locations))))
    :run-fn (fn [params]
              (let [q-params (get-in params [:query-params :protocol-version])
                    protocol-version-ids (if (coll? q-params) q-params (list q-params))
                    default-lang (:language (utils/current-org params))
                    lang-id (get-in params [:query-params :language])
                    requested-lang (if lang-id (data/find-record types/language lang-id) default-lang)]
                (map #(reformat-version-data % {:lang requested-lang :default-lang default-lang})
                     protocol-version-ids)))
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
   
   {:name "post-protocol-version-clone"
    :runnable-fn auth-designer-for-protocol-published
    :run-fn clone-version
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
                            (apply concat (map #(data/find-related-records "protocol-version" % (list "meta-item")) ids))
                            (data/find-related-records "protocol-version" ids (list "meta-item"))))))
    :run-if-false forbidden-fn}])

(process/register-processes (map #(DefaultProcess/create %) protocol-version-processes))
