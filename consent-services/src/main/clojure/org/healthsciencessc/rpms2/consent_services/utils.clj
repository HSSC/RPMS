(ns org.healthsciencessc.rpms2.consent-services.utils
  (:require [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-domain.lookup :as lookup]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]))

;; Functions for getting current user and user org
(defn current-user
  "Function used to get the current user from the request context.  Used for providing a user fetcher to runnable functions."
  [ctx]
  (get-in ctx [:session :current-user]))

(defn current-org
  [ctx]
  (get-in ctx [:session :current-user :organization]))

(defn current-org-id
  [ctx]
  (get-in ctx [:session :current-user :organization :id]))

(def lookup-organization (lookup/gen-organization-lookup-in-query current-org-id))

;; Define methods to help with process authorization
(defn record-belongs-to-user-org
  "Checks if an ID value with the query parameters belongs to a record which is linked to 
   the requestors organization.  It assumes that the ID value is stored in a parameter 
   named the same as the record type."
  [ctx record-type]
  (let [org-id (current-org-id ctx)
        record-id (get-in ctx [:query-params (keyword record-type)])]
    (data/belongs-to? record-type record-id types/organization org-id)))

(defn record-owned-by-user-org
  "Checks if an ID value with the query parameters belongs to a record which is owned by 
   the requestors organization.  It assumes that the ID value is stored in a parameter 
   named the same as the record type."
  [ctx record-type]
  (let [org-id (current-org-id ctx)
        record-id (get-in ctx [:query-params (keyword record-type)])
        record (data/find-record record-type record-id)
        record-org-id (get-in record [:organization :id])]
    (= org-id record-org-id)))

;; Generates Common Process Functions For Working With Data
(defn gen-type-delete
  "Generates a function that deletes a node using the provided type and the ID value lookup function."
  [node-type id-lookup]
  (fn [ctx]
    (let [node-id (id-lookup ctx)]
      (data/delete node-type node-id))))

(defn gen-type-update
  "Generates a function that updates a node using the provided type and the ID value lookup function."
  [node-type id-lookup]
  (fn [ctx]
    (let [node-id (id-lookup ctx)
          changes (:body-params ctx)]
      (data/update node-type node-id changes))))

(defn gen-type-create
  "Generates a function that creates a node using the provided type."
  [node-type]
  (fn [ctx]
    (let [changes (:body-params ctx)]
      (data/create node-type changes))))

(defn gen-type-records-by-user-org
  "Generates a function that looks up all nodes of the provided type for the requesting user."
  [node-type]
  (fn [ctx]
    (data/find-children types/organization (current-org-id ctx) node-type)))

(defn gen-type-records-by-org
  "Generates a function that looks up all nodes of the provided type for the requesting user."
  [node-type]
  (fn [ctx]
    (data/find-children types/organization (lookup-organization ctx) node-type)))

(defn gen-types-unrelate
  "Generates a function that will remove a relationship between two nodes."
  [c-type c-lookup p-type p-lookup]
  (fn [ctx]
    (let [p-id (p-lookup ctx)
          c-id (c-lookup ctx)]
      (data/unrelate-records c-type c-id p-type p-id))))

(defn gen-types-relate
  "Generates a function that will remove a relationship between two nodes."
  [c-type c-lookup p-type p-lookup]
  (fn [ctx]
    (let [p-id (p-lookup ctx)
          c-id (c-lookup ctx)]
      (data/relate-records c-type c-id p-type p-id))))

;; Generates Common Functions For Looking Up Data Records
(defn gen-record-lookup
  "Generates a function that will lookup a specific instance of a type, getting it's ID value from the 
   request context using the vector of path keywords."
  [node-type lookup]
  (fn [ctx]
    (let [node-id (lookup ctx)]
      (data/find-record node-type node-id))))

;; Lookups Up Specifically Requested Types From The ID Passed In The Request
(def get-endorsement-record (gen-record-lookup types/endorsement lookup/get-endorsement-in-query))
(def get-endorsement-type-record (gen-record-lookup types/endorsement-type lookup/get-endorsement-type-in-query))
(def get-form-record (gen-record-lookup types/form lookup/get-form-in-query))
(def get-group-record (gen-record-lookup types/group lookup/get-group-in-query))
(def get-language-record (gen-record-lookup types/language lookup/get-language-in-query))
(def get-location-record (gen-record-lookup types/location lookup/get-location-in-query))
(def get-meta-item-record (gen-record-lookup types/meta-item lookup/get-meta-item-in-query))
(def get-organization-record (gen-record-lookup types/organization lookup/get-organization-in-query))
(def get-policy-record (gen-record-lookup types/policy lookup/get-policy-in-query))
(def get-policy-definition-record (gen-record-lookup types/policy-definition lookup/get-policy-definition-in-query))
(def get-protocol-record (gen-record-lookup types/protocol lookup/get-protocol-in-query))
(def get-protocol-version-record (gen-record-lookup types/protocol-version lookup/get-protocol-version-in-query))
(def get-role-record (gen-record-lookup types/role lookup/get-role-in-query))
(def get-role-mapping-record (gen-record-lookup types/role-mapping lookup/get-role-mapping-in-query))
(def get-user-record (gen-record-lookup types/user lookup/get-user-in-query))
(def get-text-i18n-record (gen-record-lookup types/text-i18n lookup/get-text-i18n-in-query))
(def get-widget-record (gen-record-lookup types/widget lookup/get-widget-in-query))
