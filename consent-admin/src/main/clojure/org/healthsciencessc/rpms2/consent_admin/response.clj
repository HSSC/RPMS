(ns org.healthsciencessc.rpms2.consent-admin.response
  (require [ring.util.response :as ring]))

(defn ajax-success
  "Function called to return a successful response"
  [body]
  (ring/response body))

(defn ajax-error
  "Function called to return error."
  ([error] (ajax-error error []))
  ([error fielderrors]
    (ring/status (ring/response error) 500)))