(ns org.healthsciencessc.consent.services.process.language
  (:use     [pliant.process :only [defprocess as-method]])
  (:require [org.healthsciencessc.consent.services.data :as data]
            [org.healthsciencessc.consent.services.respond :as respond]
            [org.healthsciencessc.consent.services.session :as session]
            [org.healthsciencessc.consent.services.vouch :as vouch]
            [org.healthsciencessc.consent.common.roles :as roles]
            [org.healthsciencessc.consent.common.types :as types]
            [pliant.webpoint.request :as endpoint]))


(defn designs-language
  [ctx]
  (vouch/designs-type ctx types/language (get-in ctx [:query-params :language])))

(defn views-language
  [ctx]
  (vouch/views-type-as-designer ctx types/language (get-in ctx [:query-params :language])))


;; Anyone Can View Their Orgs Languages
(defprocess get-languages
  [ctx]
  (let [user (session/current-user ctx)]
    (if user
      (data/find-children types/organization (session/current-org-id ctx) types/language)
      (respond/forbidden))))

(as-method get-languages endpoint/endpoints "get-library-languages")


(defprocess get-language
  [ctx]
  (let [language (views-language ctx)]
    (if language
      language
      (respond/forbidden))))

(as-method get-language endpoint/endpoints "get-library-language")


(defprocess add-language
  [ctx]
  (if (vouch/designs-org? ctx)
    (let [org-id (get-in ctx [:query-params :organization])
          data (assoc (:body-params ctx) :organization {:id org-id})]
      (data/create types/language data))
    (respond/forbidden)))

(as-method add-language endpoint/endpoints "put-library-language")


(defprocess update-language
  [ctx]
  (let [language (designs-language ctx)]
    (if language
      (data/update types/language (:id language) (:body-params ctx))
      (respond/forbidden))))

(as-method update-language endpoint/endpoints "post-library-language")


(defprocess delete-language
  [ctx]
  (let [language (designs-language ctx)]
    (if language
      (data/delete types/language (:id language))
      (respond/forbidden))))

(as-method delete-language endpoint/endpoints "delete-library-language")
