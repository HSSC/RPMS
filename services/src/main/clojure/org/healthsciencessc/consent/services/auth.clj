(ns org.healthsciencessc.consent.services.auth
  (:use ring.middleware.session
        org.healthsciencessc.consent.services.session
        [pliant.process :only [defprocess]])
  (:require [clojure.data.codec.base64 :as b64]
            [org.healthsciencessc.consent.services.data :as data]
            [org.healthsciencessc.consent.domain.credentials :as credentials]
            [org.healthsciencessc.consent.domain.types :as types])
  (:import org.mindrot.jbcrypt.BCrypt))

(def ^:private hash-times 11)

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
  ([{:keys [username password realm]}] (authenticate username password realm))
  ([username password] (authenticate username password "local"))
  ([username password realm]
    (if-let [identity-node (first (filter #(and (= username (:username %)) (= realm (:realm %)))
                                          (data/get-raw-nodes types/user-identity)))]
      (if (and password identity-node (good-password? password (:password identity-node)))
        (let [user-identity (data/find-record types/user-identity (:id identity-node))
              user (:user user-identity)]
          (assoc user :identity (dissoc user-identity :user)))))))

(defn wrap-authentication
  [handler authenticate]
  (fn [request]
    (let [auth (get-in request [:headers "authorization"])
          cred (and auth
                    (decode-cred
                      (last
                        (re-find #"^Basic (.*)$" auth))))
          id (credentials/unwrap-credentials cred)]
      (if (credentials/valid? id)
          (if-let [auth-user (authenticate id)]
            (binding [*current-user* auth-user]
              (handler (add-user-to-session request auth-user)))
            unauthorized-response)
          unauthorized-response-browser))))