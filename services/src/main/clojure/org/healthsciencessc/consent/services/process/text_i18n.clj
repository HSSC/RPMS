(ns org.healthsciencessc.consent.services.process.text-i18n
  (:use     [pliant.process :only [defprocess as-method]])
  (:require [org.healthsciencessc.consent.services.data :as data]
            [org.healthsciencessc.consent.services.respond :as respond]
            [org.healthsciencessc.consent.services.vouch :as vouch]
            [org.healthsciencessc.consent.common.roles :as roles]
            [org.healthsciencessc.consent.common.types :as types]
            [pliant.webpoint.request :as endpoint]
            [borneo.core :as neo]))


(defn designs-parent
  [ctx]
  (let [parent-type (get-in ctx [:query-params :parent-type])
        parent-id (get-in ctx [:query-params :parent-id])]
    (vouch/designs-type ctx parent-type parent-id)))

(defn parent-text
  [ctx parent]
  (if parent
    (let [text-i18n-id (get-in ctx [:query-params :text-i18n])
          property (keyword (get-in ctx [:query-params :property]))
          texts (property parent)]
      (cond
        (map? texts) (if (= (:id texts) text-i18n-id) texts)
        (coll? texts) (first (filter #(= (:id %) text-i18n-id) texts))))))

(defn designs-parent-text
  [ctx]
  (let [parent (designs-parent ctx)]
    (if parent
      (parent-text ctx parent))))


(defprocess add-text-i18n
  [ctx]
  (let [parent (designs-parent ctx)]
    (if parent
      (neo/with-tx
        (let [data (assoc (:body-params ctx) :organization (:organization parent))
              text (data/create types/text-i18n data)
              parent-type (get-in ctx [:query-params :parent-type])
              property (keyword (get-in ctx [:query-params :property]))]
          (data/relate-records types/text-i18n (:id text) parent-type (:id parent) :rel-name property)
          text))
      (respond/forbidden))))

(as-method add-text-i18n endpoint/endpoints "put-library-text-i18n")


(defprocess update-text-i18n
  [ctx]
  (let [text-i18n (designs-parent-text ctx)]
    (if text-i18n
      (data/update types/text-i18n (:id text-i18n) (select-keys (:body-params ctx) [:value]))
      (respond/forbidden))))

(as-method update-text-i18n endpoint/endpoints "post-library-text-i18n")


(defprocess delete-text-i18n
  [ctx]
  (let [parent-type (get-in ctx [:query-params :parent-type])
        parent (designs-parent ctx) 
        text-i18n (parent-text ctx parent)]
    (if text-i18n
      (neo/with-tx
        (data/unrelate-records types/text-i18n (:id text-i18n) parent-type (:id parent))
        (data/delete types/text-i18n (:id text-i18n)))
      (respond/forbidden))))

(as-method delete-text-i18n endpoint/endpoints "delete-library-text-i18n")
