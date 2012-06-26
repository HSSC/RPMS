(ns org.healthsciencessc.rpms2.consent-admin.ajax
  (:require [ring.util.response :as response]
            [clojure.data.json :as json]))

(def headers {"Content-Type" "application/json"})

(defn- response
  [status body]
  {:status status
   :headers headers
   :body body})

(defn success
  "Function called to return a successful response"
  [body]
  (response 200 body))

(defn error
  "Function called to return error."
  [{msg :message :as m}]
  (response 500 {:message msg}))

(defn save-failed
  "Function called to return error."
  [{msg :message :as m}]
  (response 500 {:message msg}))

(defn lookup-failed
  "Function called to return error."
  [{msg :message :as m}]
  (response 404 {:message msg}))

(defn forbidden
  "Function called to return a forbiddent error."
  [_]
  (response 403 nil))