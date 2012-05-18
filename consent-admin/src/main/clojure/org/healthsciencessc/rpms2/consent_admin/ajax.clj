(ns org.healthsciencessc.rpms2.consent-admin.ajax
  (:require [ring.util.response :as response]))

(defn success
  "Function called to return a successful response"
  [body]
  (response/response (with-out-str (prn body)))) ;; modify to return JSON

(defn error
  "Function called to return error."
  [{msg :message :as m}]
    (response/status (response/response msg) 500))
