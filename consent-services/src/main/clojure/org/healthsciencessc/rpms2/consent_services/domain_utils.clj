(ns org.healthsciencessc.rpms2.consent-services.domain-utils
  (:use [org.healthsciencessc.rpms2.consent-domain.types]))


(defn code-in-codes?
  [code codes]
  (some (partial = code) codes))

(defn get-role-codes
  [role-mappings]
  (map #(get-in % [:role :code]) role-mappings))

(defn has-role?
  [user code]
  (code-in-codes? code (get-role-codes (:role-mappings user))))

(defn super-admin?
  [user]
  (has-role? user code-role-superadmin))

(defn admin?
  [user]
  (has-role? user code-role-admin))

(defn some-kind-of-admin?
  [user]
  (or (super-admin? user) (admin? user)))

(defn forbidden-fn
  [_]
  {:status 403})

(defn current-user
  "Function used to get the current user from the request context.  Used for providing a user fetcher to runnable functions."
  [ctx]
  (get-in ctx [:session :current-user]))
