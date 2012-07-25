(ns org.healthsciencessc.rpms2.consent-services.default-processes.organization
  (:use     [pliant.process :only [defprocess as-method]])
  (:require [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-services.respond :as respond]
            [org.healthsciencessc.rpms2.consent-services.session :as session]
            [org.healthsciencessc.rpms2.consent-services.vouch :as vouch]
            [org.healthsciencessc.rpms2.consent-domain.roles :as roles]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint]
            [borneo.core :as neo]))


;; curl -i -X GET  http://localhost:3000/security/organizations
(defprocess get-organizations
  [ctx]
  (let [user (session/current-user ctx)]
    (cond 
      (roles/superadmin? user)
        (data/find-all types/organization)
      (roles/admin? user)
        (vector (data/find-record types/organization (session/current-org-id ctx)))
      :else
        (respond/forbidden))))

(as-method get-organizations endpoint/endpoints "get-security-organizations")


;; curl -i -X GET http://localhost:3000/security/organization?organization=<ID>
(defprocess get-organization
  [ctx]
  (if (vouch/admins-org? ctx)
    (let [org-id (get-in ctx [:query-params :organization])]
      (data/find-record types/organization org-id))
    (respond/forbidden)))

(as-method get-organization endpoint/endpoints "get-security-organization")


;; curl -i -X PUT -d "{\"name\" : \"MUSC FOOBAR\"}" http://localhost:3000/security/organization
(defprocess add-organization
  [ctx]
  (if (roles/superadmin? (session/current-user ctx))
    (let [org (:body-params ctx)]
      (data/create types/organization org))
    (respond/forbidden)))

(as-method add-organization endpoint/endpoints "put-security-organization")


;; curl -i -X POST -d "{\"name\" : \"MUSC BARFOO\"}" http://localhost:3000/security/organization?organization=<ID>
(defprocess update-organization
  [ctx]
  (if (vouch/admins-org? ctx)
    (let [org-id (get-in ctx [:query-params :organization])
          org (-> ctx :body-params)]
      (data/update types/organization org-id org))
    (respond/forbidden)))

(as-method update-organization endpoint/endpoints "post-security-organization")


;; curl -i -X DELETE http://localhost:3000/security/organization?organization=<ID>
(defprocess delete-organization
  [ctx]
  (if (roles/superadmin? (session/current-user ctx))
    (let [org-id (get-in ctx [:query-params :organization])]
      (data/delete types/organization org-id))
    (respond/forbidden)))

(as-method delete-organization endpoint/endpoints "delete-security-organization")


;; curl -i -X PUT http://localhost:3000/security/organization/language?organization=<ID>&language=<ID>
(defprocess assign-language
  [ctx]
  (if (vouch/admins-org? ctx)
    (let [language-id (get-in ctx [:query-params :language])
          organization-id (get-in ctx [:query-params :organization])
          organization (data/find-record types/organization organization-id)
          language (:language organization)]
      (neo/with-tx
        (if language
          (data/unrelate-records types/organization organization-id types/language (:id language) :rel-name :language))
        (data/relate-records types/organization organization-id types/language language-id :rel-name :language))
      (data/find-record types/organization organization-id))
    (respond/forbidden)))

(as-method assign-language endpoint/endpoints "put-security-organization-language")