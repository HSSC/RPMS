(ns org.healthsciencessc.rpms2.consent-admin.error
  (:require [ring.util.response :as response]
            [clojure.data.json :as json]))

(defn- view-error
  "Called when you want to return a JSON based error message back to a non-json based request."
  [body status]
  (response/content-type (response/status (response/response (with-out-str (json/pprint-json body))) status) "application/json"))

(defn view-failure
  [body]
  (view-error body 500))

(defn view-not-found
  [body]
  (view-error body 404))

(defn view-forbidden
  [body]
  (view-error body 403))

(defn view-unauthorized
  [body]
  (view-error body 401))

(defn bad-request
  [body]
  (view-error body 400))
