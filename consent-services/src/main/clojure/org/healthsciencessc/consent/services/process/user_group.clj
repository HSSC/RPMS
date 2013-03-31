(ns org.healthsciencessc.consent.services.process.user-group
  (:use     [pliant.process :only [defprocess as-method]])
  (:require [clojure.set :as set]
            [org.healthsciencessc.consent.services.data :as data]
            [org.healthsciencessc.consent.services.process.group :as group]
            [org.healthsciencessc.consent.services.respond :as respond]
            [org.healthsciencessc.consent.domain.types :as types]
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint]))


(defprocess group-members
  [ctx]
  (if (group/admins-group? ctx)
    (let [group-id (get-in ctx [:query-params :group])]
      (data/find-children types/group group-id types/user))
    (respond/forbidden)))

(as-method group-members endpoint/endpoints "get-security-group-members")


(defprocess group-nonmembers
  [ctx]
  (if (group/admins-group? ctx)
    (let [group-id (get-in ctx [:query-params :group])
          group (data/find-record types/group group-id)
          org-id (get-in group [:organization :id])
          members (data/find-children types/group group-id types/user)
          users (data/find-children types/organization org-id types/user)]
      (set/difference (set users) (set members)))
    (respond/forbidden)))

(as-method group-nonmembers endpoint/endpoints "get-security-group-nonmembers")


(defprocess add-group-member
  [ctx]
  (if (group/admins-group? ctx)
    (let [user-id (get-in ctx [:query-params :user])
          group-id (get-in ctx [:query-params :group])]
      (data/relate-records "user" user-id "group" group-id))
    (respond/forbidden)))

(as-method add-group-member endpoint/endpoints "put-security-group-member")


(defprocess delete-group-member
  [ctx]
  (if (group/admins-group? ctx)
    (let [user-id (get-in ctx [:query-params :user])
          group-id (get-in ctx [:query-params :group])]
      (data/unrelate-records "user" user-id "group" group-id))
    (respond/forbidden)))

(as-method delete-group-member endpoint/endpoints "delete-security-group-member")
