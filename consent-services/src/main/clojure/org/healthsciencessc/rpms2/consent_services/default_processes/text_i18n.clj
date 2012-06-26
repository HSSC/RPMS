(ns org.healthsciencessc.rpms2.consent-services.default-processes.text-i18n
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils :only (forbidden-fn)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-domain.roles :as roles]
            [org.healthsciencessc.rpms2.consent-domain.lookup :as lookup]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
            [org.healthsciencessc.rpms2.consent-domain.runnable :as runnable]
            [org.healthsciencessc.rpms2.consent-services.utils :as utils]
            [borneo.core :as neo])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(defn printit
  [title obj]
  (println)
  (println "BEGIN: " title)
  (println)
  (prn obj)
  (println)
  (println "END: " title)
  (println))

(defn- auth-on-parent-type
  [ctx]
  (let [parent-type (get-in ctx [:query-params :parent-type])
        parent-id (get-in ctx [:query-params :parent-id])
        parent (data/find-record parent-type parent-id)]
    (if (runnable/can-design-org-id (utils/current-user ctx) (get-in parent [:organization :id]))
      parent)))

(defn- auth-on-parent-type-text
  [ctx]
  (let [parent (auth-on-parent-type ctx)]
    (if parent
      (let [text-i18n-id (get-in ctx [:query-params :text-i18n])
            property (keyword (get-in ctx [:query-params :property]))
            texts (property parent)]
        (cond
          (map? texts) (= (:id texts) text-i18n-id)
          (coll? texts) (some #(= (:id %) text-i18n-id) texts))))))

(defn add-text-i18n
  [ctx]
  (neo/with-tx
    (let [text (data/create types/text-i18n (:body-params ctx))
          text-i18n-id (:id text)
          parent-type (get-in ctx [:query-params :parent-type])
          parent-id (get-in ctx [:query-params :parent-id])
          property (keyword (get-in ctx [:query-params :property]))]  
    ;;  (data/relate-records types/text-i18n text-i18n-id parent-type parent-id)
      (printit "Text" text)
      (printit "Property" property)
      (data/relate-records types/text-i18n text-i18n-id parent-type parent-id :rel-name property)
      text)))

(defn update-text-i18n
  [ctx]
  (let [body (:body-params ctx)
        body (select-keys body [:value])
        text-i18n-id (get-in ctx [:query-params :text-i18n])] 
      (data/update types/text-i18n text-i18n-id body)))

(defn delete-text-i18n
  [ctx]
  (let [parent-type (get-in ctx [:query-params :parent-type])
        parent-id (get-in ctx [:query-params :parent-id])
        text-i18n-id (get-in ctx [:query-params :text-i18n])]
    (neo/with-tx
      (data/unrelate-records types/text-i18n text-i18n-id parent-type parent-id)
      (data/delete types/text-i18n text-i18n-id))))

(def language-processes
  [
   {:name "put-library-text-i18n"
    :runnable-fn  auth-on-parent-type
    :run-fn add-text-i18n
    :run-if-false forbidden-fn}

   {:name "post-library-text-i18n"
    :runnable-fn  auth-on-parent-type-text
    :run-fn update-text-i18n
    :run-if-false forbidden-fn}

   {:name "delete-library-text-i18n"
    :runnable-fn  auth-on-parent-type-text
    :run-fn delete-text-i18n
    :run-if-false forbidden-fn}])

(process/register-processes (map #(DefaultProcess/create %) language-processes))