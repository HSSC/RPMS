;; Provides the reusable runnable? functions that work off the domain functions.
(ns org.healthsciencessc.rpms2.consent-domain.runnable
  (:require [org.healthsciencessc.rpms2.consent-domain.roles :as roles]))

(defn gen-designer-location-check
  "Creates a runnable? function that checks if a user is a protocol designer for a specific location."
  [userfn]
  (fn [ctx]
    (let [user (userfn ctx)
          locationid (get-in ctx [:query-params :location])]
    (roles/protocol-designer? user :location {:id locationid}))))

(defn gen-collector-location-check
  "Creates a runnable? function that checks if a user is a consent collector for a specific location."
  [userfn]
  (fn [ctx]
    (let [user (userfn ctx)
          locationid (get-in ctx [:query-params :location])]
    (roles/consent-collector? user :location {:id locationid}))))

(defn gen-super-or-admin
  "Creates a runnable? function that checks if a user is a super admin or an admin"
  [userfn]
  (fn [ctx]
    (let [user (userfn ctx)]
      (or (roles/superadmin? user) (roles/admin? user)))))

(defn gen-super-or-admin-by-org
  "Creates a runnable? function that checks if a user is a super admin or an admin for an optional organziation."
  [userfn]
  (fn [ctx]
    (let [user (userfn ctx)
          orgid (get-in ctx [:query-params :organization])
          userorgid (get-in user [:organization :id])]
      (or (roles/superadmin? user)
          (and (roles/admin? user) 
               (or (nil? orgid)
                   (= orgid userorgid)))))))