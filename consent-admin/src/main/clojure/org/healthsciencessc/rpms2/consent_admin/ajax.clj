(ns org.healthsciencessc.rpms2.consent-admin.ajax
  (:require [ring.util.response :as response]))

(defn success
  "Function called to return a successful response"
  [body]
  body)


(defn service-failed
  "Function called to return error."
  [resp]
  (cond 
    (get-in resp [:body :message])
      (response/status (response/response {:message (get-in resp [:body :message])}) 500))
    :else
      (response/status (response/response {:message (:message (meta resp))}) 500))

(defn error
  "Function called to return error."
  [{msg :message :as m}]
  (response/status (response/response {:message msg}) 500))

(defn save-failed
  "Function called to return error."
  [{msg :message :as m}]
  (response/status (response/response {:message msg}) 500))

(defn lookup-failed
  "Function called to return error."
  [{msg :message :as m}]
  (response/not-found {:message msg}))

(defn forbidden
  "Function called to return a forbiddent error."
  [_]
  (response/status (response/response {}) 403))