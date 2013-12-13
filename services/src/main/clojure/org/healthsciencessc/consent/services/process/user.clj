(ns org.healthsciencessc.consent.services.process.user
  (:use     [pliant.process :only [defprocess as-method]])
  (:require [org.healthsciencessc.consent.services.auth :as auth]
            [org.healthsciencessc.consent.services.data :as data]
            [org.healthsciencessc.consent.services.respond :as respond]
            [org.healthsciencessc.consent.services.session :as session]
            [org.healthsciencessc.consent.services.vouch :as vouch]
            [org.healthsciencessc.consent.common.roles :as roles]
            [org.healthsciencessc.consent.common.types :as types]
            [pliant.webpoint.request :as endpoint]
            [clojure.tools.logging :as logging]
            [borneo.core :as neo]))

(defn admins-user?
  [ctx]
  (let [user (session/current-user ctx)
        user-org-id (get-in user [:organization :id])
        user-id (get-in ctx [:query-params :user])]
    (or (roles/superadmin? user)
        (and (roles/admin? user) 
             (data/belongs-to? types/user user-id types/organization user-org-id false)))))

;; curl -i -X GET http://user:password@localhost:8080/security/authenticate
(defprocess authenticate
  [ctx]
  (session/current-user ctx))

(as-method authenticate endpoint/endpoints "get-security-authenticate")

;; curl -i -X GET http://user:password@localhost:8080/security/users
(defprocess get-users
  [ctx]
  (let [user (session/current-user ctx)
        org-id (get-in ctx [:query-params :organization])]
    (cond
      (roles/superadmin? user)
        (data/find-children types/organization (or org-id (session/current-org-id ctx)) types/user)
      (roles/admin? user)
        (data/find-children types/organization (session/current-org-id ctx) types/user)
      :else
        (respond/forbidden))))

(as-method get-users endpoint/endpoints "get-security-users")

;; curl -i -X GET http://user:password@localhost:8080/security/user?user=<ID>
(defprocess get-user
  [ctx]
  (if (admins-user? ctx)
    (let [user-id (get-in ctx [:query-params :user])]
      (data/find-record types/user user-id))
    (respond/forbidden)))

(as-method get-user endpoint/endpoints "get-security-user")


(defprocess create-user
  "Process used to create"
  [organization-id raw-user & roles]
  (let [org {:id organization-id}
        raw-identity (:identity raw-user)
        rare-user (assoc (dissoc raw-user :identity) :organization org)
        rare-identity (assoc raw-identity 
                             :password (auth/hash-password (:password raw-identity))
                             :realm (or (:realm raw-identity) "local"))]
    (neo/with-tx
        (let [well-user (data/create types/user rare-user)
              well-identity (data/create types/user-identity (assoc rare-identity :user well-user))]
          (doseq [role roles]
            (data/create types/role-mapping {:organization org :role role :user well-user}))
          (assoc well-user :identity well-identity)))))

;; curl -i -X PUT -d "{:first-name  \"MUSC FOOBAR\"}" http://user:password@localhost:8080/security/user?user=<ID>
(defprocess add-user
  [ctx]
  (if (vouch/admins-org? ctx)
    (create-user (get-in ctx [:query-params :organization]) (:body-params ctx))
    (respond/forbidden)))

(as-method add-user endpoint/endpoints "put-security-user")


;; curl -i -X PUT -d "{:first-name  \"MUSC FOOBAR\"}" http://user:password@localhost:8080/security/user/admin?user=<ID>
(defprocess add-user-admin
  [ctx]
  (if (vouch/admins-org? ctx)
    (let [user-data (:body-params ctx)
          unhashed-pwd (:password user-data)
          user (assoc user-data :password (auth/hash-password unhashed-pwd))
          org-id (get-in ctx [:query-params :organization])
          admin-id (-> 
                     (filter #(= (:code %) types/code-role-admin)
                             (data/find-children types/organization org-id types/role))
                     first
                     :id)]
      (neo/with-tx
        (let [user (data/create types/user (assoc user :organization {:id org-id}))
              mapping (data/create types/role-mapping {:organization {:id org-id}
                                                       :role {:id admin-id}
                                                       :user user})]
          user)))
    (respond/forbidden)))

(as-method add-user-admin endpoint/endpoints "put-security-user-admin")


(defprocess update-user
  [ctx]
  (if (admins-user? ctx)
    (let [user-id (get-in ctx [:query-params :user])
          user-data (:body-params ctx)
          password (:password user-data)
          user-data (if password
                      (assoc user-data :password (auth/hash-password password))
                      user-data)]
      (data/update types/user user-id user-data))
    (respond/forbidden)))

(as-method update-user endpoint/endpoints "post-security-user")


(defprocess delete-user
  [ctx]
  (if (admins-user? ctx)
    (let [user-id (get-in ctx [:query-params :user])]
      (data/delete types/user user-id))
    (respond/forbidden)))

(as-method delete-user endpoint/endpoints "delete-security-user")


