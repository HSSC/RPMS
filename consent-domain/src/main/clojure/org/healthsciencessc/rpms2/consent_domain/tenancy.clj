;; Provides the reusable functions for dealing with our tenancy.
(ns org.healthsciencessc.rpms2.consent-domain.tenancy)

(defn only-my-org
  [user coll]
  (let [org-id (get-in user [:organization :id])]
    (filter #(= org-id (get-in % [:organization :id])) coll)))
