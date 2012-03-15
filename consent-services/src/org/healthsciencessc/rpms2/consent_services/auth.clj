(ns org.healthsciencessc.rpms2.consent-services.auth
  (:use ring.middleware.session)
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [clojure.data.codec.base64 :as b64])
  (:import org.mindrot.jbcrypt.BCrypt))

(defn authenticate?
  [username password]
  (process/dispatch "authenticate" {:username username :password password}))

(def ^:private hash-times 3)

;; Using bcrypt for hashing
;; http://codahale.com/how-to-safely-store-a-password/
(defn generate-salt 
  []
  (BCrypt/gensalt))

(defn hash-password
  [password salt]
  (loop [pass password count hash-times]
    (if (= 0 count)
      pass
      (recur (BCrypt/hashpw pass salt) (dec count)))))

(def unauthorized-response
  {:status 401
   :body "Access Denied"
   :headers {"Content-Type" "text/plain"}})

(defn add-user-to-session
  [request user]
  (assoc-in request :session :user user))

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
      (if-let [auth-user (authenticate? user pass)]
        (handler (add-user-to-session request auth-user))
        unauthorized-response))))