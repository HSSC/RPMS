(ns org.healthsciencessc.rpms2.consent-services.auth
  (:use ring.middleware.session)
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [clojure.data.codec.base64 :as b64]))

(defn authenticate?
  [username password]
  (process/dispatch "authenticate" {:username username :password password}))

(def unauthorized-response
  {:status 401
   :body "Access Denied"
   :headers {"Content-Type" "text/plain"}})

(defn decode-cred
  [cred]
  (-> cred .getBytes b64/decode String.))

(defn wrap-authentication
  [handler]
  (fn [request]
    (let [auth ((:headers request) "authorization")
          cred (and auth
                    (decode-cred
                     (last
                      (re-find #"^Basic (.*)$" auth))))
          user (and cred
                    (last
                     (re-find #"^(.*):" cred)))
          pass (and cred
                    (last
                     (re-find #":(.*)$" cred)))]
      (if (authenticate? user pass)
        (handler request)
        unauthorized-response))))