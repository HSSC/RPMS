(ns org.healthsciencessc.rpms2.consent-services.auth
  (:use ring.middleware.session
        org.healthsciencessc.rpms2.consent-services.session
        [pliant.process :only [defprocess]])
  (:require [clojure.data.codec.base64 :as b64]
            [org.healthsciencessc.rpms2.consent-services.data :as data])
  (:import org.mindrot.jbcrypt.BCrypt))

(def ^:private hash-times 11)

;;(def ^:dynamic *current-user*)

;; Using bcrypt for hashing
;; http://codahale.com/how-to-safely-store-a-password/
(defn generate-salt 
  []
  (BCrypt/gensalt (int hash-times)))

(defn hash-password
  [password]
  (BCrypt/hashpw password (generate-salt)))

(defn good-password?
  [candidate hashed]
  (BCrypt/checkpw candidate hashed))

(def unauthorized-response-browser
  {:status 401
   :body "Access Denied"
   :headers {"Content-Type" "text/plain" "WWW-Authenticate" "Basic"}})

(def unauthorized-response
  {:status 401
   :body "Access Denied"
   :headers {"Content-Type" "text/plain"}})

(defn add-user-to-session
  [request user]
  (assoc-in request [:session :current-user] user))

(defn decode-cred
  [cred]
  (-> cred .getBytes b64/decode String.))

(defprocess authenticate
  [username password]
  (if-let [user-node (first (filter #(= username (:username %))
                                    (data/get-raw-nodes "user")))]
    (if (and password user-node (good-password? password (:password user-node)))
      (first (data/find-records-by-attrs "user" {:username username})))))

#_(defn authenticate
  [username password]
  (process/dispatch "authenticate" {:username username :password password}))

(defn wrap-authentication
  [handler authenticate]
  (fn [request]
    (let [auth (get-in request [:headers "authorization"])
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
      (if (and user pass)
          (if-let [auth-user (authenticate user pass)]
            (binding [*current-user* auth-user]
              (handler (add-user-to-session request auth-user)))
            unauthorized-response)
          unauthorized-response-browser))))