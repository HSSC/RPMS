(ns org.healthsciencessc.consent.services.process.widget
  (:use     [pliant.process :only [defprocess as-method]])
  (:require [org.healthsciencessc.consent.services.data :as data]
            [org.healthsciencessc.consent.services.respond :as respond]
            [org.healthsciencessc.consent.services.vouch :as vouch]
            [org.healthsciencessc.consent.common.roles :as roles]
            [org.healthsciencessc.consent.common.types :as types]
            [pliant.webpoint.request :as endpoint]
            [borneo.core :as neo]))

(defn dissoc-empty
  [m k]
  (if (empty? (m k))
    (dissoc m k)
    m))

(defprocess update-designer-form
  [ctx]
  (let [protocol-version (vouch/designs-protocol-version ctx)]
    (if (and protocol-version (types/draft? protocol-version))
      (let [body (:body-params ctx)
            form-id (get-in ctx [:query-params :form])
            form (dissoc-empty (get-in body[:update :form]) :witness-signatures)]
        (neo/with-tx
          (if form (data/update types/form form-id form))
          (doseq [text (get-in body[:create :title])] 
            (let [node (data/create types/text-i18n text)]
              (data/relate-records types/text-i18n (:id node) types/form form-id)))
          (doseq [text (get-in body[:update :title])] 
            (data/update types/text-i18n (:id text) text))
          (doseq [text (get-in body[:delete :title])] 
            (data/delete types/text-i18n (:id text))))
        (dissoc (data/find-record types/form form-id) :contains))
      (respond/forbidden))))

(as-method update-designer-form endpoint/endpoints "post-designer-form")


(defprocess create-designer-form-widget
  [ctx]
  (let [protocol-version (vouch/designs-protocol-version ctx)]
    (if (and protocol-version (types/draft? protocol-version))
      (let [widget (:body-params ctx)
            widget-id (get-in ctx [:query-params :widget])
            form-id (get-in ctx [:query-params :form])
            widget1 (if widget-id (merge widget {:contained-in {:id widget-id}})
                      (merge widget {:form {:id form-id}}))
            widget2 (assoc widget1 :organization (:organization protocol-version))]
        (data/create-records types/widget widget2))
      (respond/forbidden))))

(as-method create-designer-form-widget endpoint/endpoints "put-designer-form-widget")


(defprocess update-designer-form-widget
  [ctx]
  (let [protocol-version (vouch/designs-protocol-version ctx)]
    (if (and protocol-version (types/draft? protocol-version))
      (let [body (:body-params ctx)
            widget-id (get-in ctx [:query-params :widget])
            widget (data/find-record types/widget widget-id)
            record (get-in body[:update :widget])]
        (neo/with-tx
          (cond
            (sequential? record)
            (doseq [node record]
              (data/update types/widget (:id node) node))
            record
            (data/update types/widget widget-id record))
          (doseq [prop (get-in body[:create :property])] 
            (let [prop1 (assoc prop :organization (:organization widget))
                  prop2 (assoc prop1 :widget widget)]
              (data/create types/widget-property prop2)))
          (doseq [prop (get-in body[:update :property])] 
            (data/update types/widget-property (:id prop) prop))
          (doseq [prop (get-in body[:delete :property])] 
            (data/delete types/widget-property (:id prop))))
        (dissoc (data/find-record types/widget widget-id) :contains))
      (respond/forbidden))))

(as-method update-designer-form-widget endpoint/endpoints "post-designer-form-widget")


(defprocess delete-designer-form-widget
  [ctx]
  (let [protocol-version (vouch/designs-protocol-version ctx)]
    (if (and protocol-version (types/draft? protocol-version))
      (let [widget-id (get-in ctx [:query-params :widget])]
        (data/delete types/widget widget-id))
      (respond/forbidden))))

(as-method delete-designer-form-widget endpoint/endpoints "delete-designer-form-widget")
