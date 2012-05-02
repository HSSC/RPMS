(ns org.healthsciencessc.rpms2.consent-services.seed
  (:require [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-services.auth :as auth]
            [org.healthsciencessc.rpms2.consent-domain.core :as domain])
  (:import [java.util Locale]))

(defn setup-default-schema!
  []
  (data/setup-schema domain/default-data-defs))

(def remove-keys #{:id :password})

(defn bare-record [r]
  "Removes id's maps and optional extra keywords"
  (if (map? r)
    (into {}
          (for [[k v] r :when
                (not (or (coll? v) (contains? remove-keys k)))]
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

(defn update-cache [type record]
  (swap! db-cache update-in [type] conj record))

(defn create
  "Makes or finds"
  [type props]
  (if-let [record (exists-in-db? type props)]
    record
    (let [record (data/create type props)]
      (update-cache type record)
      record)))

(defn- create-roles [def-org]
  (doseq [[name code] [["Super Administrator" "sadmin"]
                       ["Administrator" "admin"]
                       ["Consent Collector" "collect"]
                       ["Consent Designer" "design"]
                       ["Consent Manager" "manage"]
                       ["Consent System" "csys"]]]
    (create "role" {:name name
                    :code code
                    :organization def-org}))
  nil)

(defn- create-langs [def-org]
  (doseq [^Locale lc (Locale/getAvailableLocales)]
    (create "language"
            {:name (.getDisplayName lc)
             :code (.getLanguage lc)
             :organization def-org})))

(defn- create-org []
  (create "organization"
          {:name "Default Organization"
           :code "deforg"}))

(defn- get-role-by-code
  [code]
  (first (data/find-records-by-attrs "role" {:code code})))

(defn- create-users [def-org]
  (let [super-admin (create "user"
                            {:first-name "Super"
                             :last-name "Administrator"
                             :username "admin"
                             :password (auth/hash-password "root")
                             :organization def-org})]
    (create "role-mapping"
            {:organization def-org
             :role (get-role-by-code "sadmin")
             :user super-admin})))

(defn seed-graph! []
  (let [def-org (create-org)]
    (create-roles def-org)
    (create-users def-org)
    (create-langs def-org)))

(defn create-test-nodes!
  []
  (let [org (data/create "organization" {:name "MUSC"})
        org-id (:id org)
        reg-desk (data/create "location" {:name "Registration Desk" :organization org})
        cafeteria (data/create "location" {:name "Cafeteria" :organization org})
        library (data/create "location" {:name "Library" :organization org})
        lab (data/create "location" {:name "Research Lab" :organization org})
        jimbo (data/create "user" {:username "jimbo" :password (auth/hash-password "foobar") :organization org})
        juan (data/create "user" {:username "juan" :password (auth/hash-password "foobar") :organization org})
        jane (data/create "user" {:username "jane" :password (auth/hash-password "foobar") :organization org})
        consent-mgr-grp (data/create "group" {:name "Consent Managers" :organization org})
        collector-role (get-role-by-code "collect")]
    (data/create "role-mapping" {:organization org :user jimbo :role collector-role})
    (data/create "role-mapping" {:organization org :user juan :role collector-role :location reg-desk})
    (doseq [loc (list reg-desk cafeteria library lab)]
      (data/create "role-mapping" {:organization org :user jane :role collector-role :location loc}))
    (data/create "role-mapping" {:organization org :group consent-mgr-grp :role (get-role-by-code "manage")})
    (data/relate-records "user" (:id jimbo) "group" (:id consent-mgr-grp))))

(defn reset-dev-db!
  []
  (do
    (println "Deleting all data...")
    (data/delete-all-nodes!)
    (println "Setting up schema...")
    (setup-default-schema!)
    (println "Seeding default data...")
    (seed-graph!)
    (println "Seeding test data...")
    (create-test-nodes!)
    "Done!"))
