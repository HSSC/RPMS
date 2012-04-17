(ns org.healthsciencessc.rpms2.consent-services.domain-utils)

(def super-admin-role-code
  "sadmin")

(def admin-role-code
  "admin")

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
  (has-role? user super-admin-role-code))

(defn admin?
  [user]
  (has-role? user admin-role-code))

(defn some-kind-of-admin?
  [user]
  (or (super-admin? user) (admin? user)))

(defn forbidden-fn
  [_]
  {:status 403})