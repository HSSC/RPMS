;; Provides the reusable runnable? functions that work off the domain functions.
(ns org.healthsciencessc.rpms2.consent-domain.runnable
  (:require [org.healthsciencessc.rpms2.consent-domain.roles :as roles]))

;; Helper functions to check if a location is accessible for a user.
(defn can-collect-location-id
  [user location-id]
  (roles/consent-collector? user :location {:id location-id}))

(defn can-collect-location
  [user location]
  (can-collect-location-id user (:id location)))

;; Helper functions to check if a protocol/version/location is accessible for a user.
(defn can-design-location-id
  [user location-id]
  (roles/protocol-designer? user :location {:id location-id}))

(defn can-design-location
  [user location]
  (can-design-location-id user (:id location)))

(defn can-design-protocol
  [user protocol]
  (can-design-location user (:location protocol)))

(defn can-design-protocol-version
  [user protocol-version]
  (can-design-protocol user (:protocol protocol-version)))

;; Functions that generate functions used in runnable statements.
(defn gen-designer-check
  "Creates a runnable? function that checks if a user is a protocol designer for a specific location."
  [userfn]
  (fn [ctx]
    (let [user (userfn ctx)]
      (roles/protocol-designer? user))))

(defn gen-collector-check
  "Creates a runnable? function that checks if a user is a consent collector for a specific location."
  [userfn]
  (fn [ctx]
    (let [user (userfn ctx)]
      (roles/consent-collector? user))))

(defn gen-designer-location-check
  "Creates a runnable? function that checks if a user is a protocol designer for a specific location."
  [userfn path-to-location]
  (fn [ctx]
    (let [user (userfn ctx)
          location-id (get-in ctx path-to-location)]
      (can-design-location-id user location-id))))

(defn gen-collector-location-check
  "Creates a runnable? function that checks if a user is a consent collector for a specific location."
  [userfn path-to-location]
  (fn [ctx]
    (let [user (userfn ctx)
          location-id (get-in ctx path-to-location)]
      (can-collect-location-id user location-id))))

(defn gen-super-or-admin
  "Creates a runnable? function that checks if a user is a super admin or an admin"
  [userfn]
  (fn [ctx]
    (let [user (userfn ctx)]
      (or (roles/superadmin? user) (roles/admin? user)))))

(defn gen-super-or-admin-by-org
  "Creates a runnable? function that checks if a user is a super admin or an admin for an optional organziation."
  [userfn path-to-organization]
  (fn [ctx]
    (let [user (userfn ctx)
          orgid (get-in ctx path-to-organization)
          userorgid (get-in user [:organization :id])]
      (or (roles/superadmin? user)
          (and (roles/admin? user) 
               (or (nil? orgid)
                   (= orgid userorgid)))))))