(ns org.healthsciencessc.rpms2.consent-services.seed
  (:require [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-services.auth :as auth]
            [org.healthsciencessc.rpms2.consent-domain.core :as domain])
  (:import [java.util Locale]))

(defn setup-default-schema!
  []
  (data/setup-schema domain/default-data-defs))

(defn bare-record [r]
  (if (map? r)
    (into {}
          (for [[k v] r :when
            (not (or (map? v) (= :id k)))]
              [k v]))))

(def db-cache (atom {}))

(defn fill-cache! [type]
  (letfn [(assoc-cache [c type]
             (with-meta (assoc c type
                               (data/find-all type))
                        (assoc (meta c) type true)))]
    (swap! db-cache assoc-cache type)))

(defn get-cache [type]
  (let [cache-meta (meta @db-cache)]
    (if-not (get cache-meta type)
      (fill-cache! type)))
  (get @db-cache type))
  
(defn exists-in-db? [type props]
  (let [record (bare-record props)
        cache (get-cache type)]
    (some (fn [x]
            (if (= record (bare-record x))
              x))
          cache)))

(defn create
  [type props]
  (or (exists-in-db? type props)
      (data/create type props)))

(defn- create-roles [def-org]
  (doseq [[name code] [["Super Administrator" "sadmin"]
                       ["Administrator" "admin"]
                       ["Consent Collector" "collect"]
                       ["Consent Designer" "design"]
                       ["Consent Manager" "manage"]
                       ["Consent System" "csys"]]]
    (create "role" {:name name
                    :code code
                    :organization {:id def-org}}))
  nil)

(defn- create-langs [def-org]
  (doseq [^Locale lc (Locale/getAvailableLocales)]
    (create "language"
            {:name (.getDisplayName lc)
             :code (.getLanguage lc)
             :organization {:id def-org}})))

(defn- create-org []
  (:id (create "organization"
               {:name "Default Organization"
                :code "deforg"})))

(defn- get-role-id-by-code
  [code]
  (-> (data/find-records-by-attrs "role"
                             {:code code})
      first
      :id))

(defn- create-users [def-org]
  (let [super-admin (:id (create "user"
                                 {:first-name "Super"
                                  :last-name "Administrator"
                                  :username "admin"
                                  :password (auth/hash-password "root")
                                  :organization {:id def-org}}))]
    (create "role-mapping"
            {:organization {:id def-org}
             :role {:id (get-role-id-by-code "sadmin")}
             :user {:id super-admin}})))

(defn seed-graph! []
  (let [def-org (create-org)]
    (create-roles def-org)
    (create-users def-org)
    (create-langs def-org)))

(defn create-test-nodes
  []
  (let [org (create "organization" {:name "MUSC"})
        org-id (:id org)
        location (create "location" {:name "Registration Desk" :organization {:id (:id org)}})
        collector (create "user" {:username "collector" :password (auth/hash-password "foobar")
                                  :organization {:id org-id}})
        admin (create "user" {:username "musc-admin" :password (auth/hash-password "foobar")
                              :organization {:id org-id}})]
    (create "role-mapping" {:organization {:id (:id org)}
                            :role {:id (get-role-id-by-code "collect")}
                            :user {:id (:id collector)}
                            :location {:id (:id location)}})
    (create "role-mapping" {:organization {:id (:id org)}
                            :role {:id (get-role-id-by-code "admin")}
                            :user {:id (:id admin)}
                            :location {:id (:id location)}})))
