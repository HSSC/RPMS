(ns org.healthsciencessc.rpms2.consent-admin.download
  (:require [ring.util.response :as response]))

(defn success-string
  "Function called to return a successful response"
  [^String body contentType fileName]
  (let [bites (.getBytes body)]
    (-> (java.io.ByteArrayInputStream. bites)
      (response/response)
      (response/content-type contentType)
      (response/header "Content-Disposition" (str "attachment; filename=" fileName))
      (response/header "Content-Encoding" "none")
      (response/header "Content-Length" (count bites)))))

(defn error
  "Function called to return error."
  [message]
  (response/status (response/response message 500)))

(defn service-error
  "Function called to return error when error occurs within services."
  [{msg :message :as m}]
  (response/status (response/response {:message msg}) 500))
