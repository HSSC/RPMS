(ns org.healthsciencessc.consent.services.process.user
  (:use     [pliant.process :only [defprocess as-method]])
  (:require [org.healthsciencessc.consent.services.auth :as auth]
            [org.healthsciencessc.consent.services.data :as data]
            [org.healthsciencessc.consent.services.respond :as respond]
            [org.healthsciencessc.consent.services.session :as session]
            [org.healthsciencessc.consent.services.vouch :as vouch]
            [org.healthsciencessc.consent.domain.roles :as roles]
            [org.healthsciencessc.consent.domain.types :as types]
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint]
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

;; curl -i -X PUT -d "{:first-name  \"MUSC FOOBAR\"}" http://user:password@localhost:8080/security/user?user=<ID>
(defprocess add-user
  [ctx]
  (if (vouch/admins-org? ctx)
    (let [user-data (:body-params ctx)
          unhashed-pwd (:password user-data)
          user (assoc user-data :password (auth/hash-password unhashed-pwd))
          org-id (get-in ctx [:query-params :organization])]
      (data/create types/user (assoc user :organization {:id org-id})))
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


