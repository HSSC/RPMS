;; Provides common lookup functions and generation functions for working with data in the request context.
(ns org.healthsciencessc.rpms2.consent-domain.lookup)

;; Generates Common Functions For Looking Up Data In Request Contexts
(defn gen-request-lookup
  "Generates a function that will lookup a specific value from the request and return it."
  [path-to-id]
  (fn [ctx]
      (get-in ctx path-to-id)))

(defn gen-request-query-lookup
  "Generates a function that will lookup a specific value from the request query params and return it."
  [path-to-id]
  (gen-request-lookup (concat [:query-params] path-to-id)))

(defn gen-request-body-lookup
  "Generates a function that will lookup a specific value from the request body params and return it."
  [path-to-id]
  (gen-request-lookup (concat [:body-params] path-to-id)))

;; Commonly Used Generated Lookup Functions For Types
;; Lookups the search for the ID of the type in the request params using the common naming pattern.
(def get-endorsement-in-query (gen-request-query-lookup [:endorsement]))
(def get-endorsement-type-in-query (gen-request-query-lookup [:endorsement-type]))
(def get-group-in-query (gen-request-query-lookup [:group]))
(def get-language-in-query (gen-request-query-lookup [:language]))
(def get-location-in-query (gen-request-query-lookup [:location]))
(def get-meta-item-in-query (gen-request-query-lookup [:meta-item]))
(def get-organization-in-query (gen-request-query-lookup [:organization]))
(def get-policy-in-query (gen-request-query-lookup [:policy]))
(def get-protocol-in-query (gen-request-query-lookup [:protocol]))
(def get-protocol-version-in-query (gen-request-query-lookup [:protocol-version]))
(def get-role-in-query (gen-request-query-lookup [:role]))
(def get-role-mapping-in-query (gen-request-query-lookup [:role-mapping]))
(def get-user-in-query (gen-request-query-lookup [:user]))
(def get-widget-in-query (gen-request-query-lookup [:widget]))

;; Lookups For Inspecting The Request Body
(def get-organization-in-body (gen-request-body-lookup [:organization :id]))

;; Create Organization ID Lookup Generators
(defn gen-organization-lookup
  [current-orig-id-fn]
  (fn [ctx]
    (first (filter identity (for [x [get-organization-in-query get-organization-in-body current-orig-id-fn]]
                     (x ctx))))))

(defn gen-organization-lookup-in-query
  [current-orig-id-fn]
  (fn [ctx]
    (first (filter identity (for [x [get-organization-in-query current-orig-id-fn]]
                     (x ctx))))))

(defn gen-organization-lookup-in-body
  [current-orig-id-fn]
  (fn [ctx]
    (first (filter identity (for [x [get-organization-in-body current-orig-id-fn]]
                     (x ctx))))))