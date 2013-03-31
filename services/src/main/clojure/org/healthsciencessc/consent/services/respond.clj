(ns org.healthsciencessc.consent.services.respond
  (:require [ring.util.response :as ring]))

(defn with-xml
  [xml] (ring/content-type (ring/response xml) "text/xml"))

(defn not-found
  ([] (not-found "The requested resource was not found."))
  ([message] (ring/not-found {:message message})))

(defn forbidden
  ([] (forbidden "You do not have the authority to execute the requested process."))
  ([message]
    (ring/status (ring/response {:message message}) 403)))