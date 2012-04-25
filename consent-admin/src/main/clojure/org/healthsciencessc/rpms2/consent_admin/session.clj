;; Provides the configuration of the admin
(ns org.healthsciencessc.rpms2.consent-admin.session
  (use (sandbar stateful-session)))


(defn- persist
  [k v]
  (session-put! :context (assoc (session-get :context {}) k v)))

(defn- obtain
  ([k] obtain k nil)
  ([k d]
    ((session-get :context {}) k d)))

(defn put-user
  "Puts the user in the session"
  [user]
  (persist :user user))

(defn get-user
  ""
  []
  (obtain :user))

(defn session-valid?
  ""
  []
  (not (nil? (get-user))))