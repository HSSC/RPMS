(ns org.healthsciencessc.consent.services.upgrade
  "Provides an entry point for updating the database with schema changes."
  (:require [org.healthsciencessc.consent.services.data :as data]
            [org.healthsciencessc.consent.services.seed :as seed]
            [org.healthsciencessc.consent.common.types :as types]
            [clojure.tools.logging :as logging]
            [borneo.core :as neo]))


(defn update-credentials-to-user-identities
  []
  (let [uids (data/find-all types/user-identity)]
    (if (nil? (seq uids))
      (let [users (data/get-raw-nodes types/user)]
        (doseq [user users]
          (logging/info "Updating User To Identity: " user)
          (data/create types/user-identity 
                       {:realm "local" :user user :password (:password user) 
                        :username (:username user)}))))))

(def updates [{:version 0 :fn seed/seed}
              {:version 1 :fn update-credentials-to-user-identities}])

(defn check-version
  []
  (let [system-prop (first (filter #(= "version" (:key %1)) (data/find-all types/system)))
        version (or (:value system-prop) -1)
        sorted-updates (seq (sort-by :version (filter #(> (:version %) version) updates)))]
    (logging/info "Current Version: " version)
    (logging/info "Available Updates: " (count sorted-updates))
    (if sorted-updates
      (neo/with-tx 
        (loop [updates sorted-updates
               last-version version]
          (if (nil? updates)
            (if (nil? system-prop)
              (data/create types/system {:key "version" :value last-version})
              (data/update types/system (:id system-prop) (assoc system-prop :value last-version)))
            (let [update (first updates)]
              ((:fn update))
              (recur (next updates) (:version update)))))))))