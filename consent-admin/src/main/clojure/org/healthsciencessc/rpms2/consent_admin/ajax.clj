(ns org.healthsciencessc.rpms2.consent-admin.ajax
  (:use [ring.util.response :as response]))

(defn success
  "Function called to return a successful response"
  [body]
  body)

(defn error
  "Function called to return error."
  [{msg :message :as m}]
  (status (response/response {:message msg}) 500))

(defn save-failed
  "Function called to return error."
  [{msg :message :as m}]
  (status (response/response {:message msg}) 500))

(defn lookup-failed
  "Function called to return error."
  [{msg :message :as m}]
  (response/not-found {:message msg}))

(defn forbidden
  "Function called to return a forbiddent error."
  [_]
  (status (response/response {}) 403))