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
  "Removes id's, optional extra keywords, and removes all but :id's from map values."
  (if (map? r)
    (into {}
          (for [[k v] r :when
                (not (contains? remove-keys k))]
            (if (coll? v)
              [k {:id (:id v)}]
              [k v])))))

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

(defn exists-in-db? 
  "Checks if a type that is trying to be created already exists."
  [type props]
  (let [record (bare-record props)
        cache (get-cache type)]
    (some (fn [x]
            (if (= record (bare-record x))
              x))
          cache)))

(defn update-cache 
  "Puts a type record in the seed cache after it has been created."
  [type record]
  (swap! db-cache update-in [type] conj record))

(defn create
  "Makes or finds a type in the database.  Allows for the seeding of data on startup without the risk of duplication."
  [type props]
  (if-let [record (exists-in-db? type props)]
    record
    (let [record (data/create type props)]
      (update-cache type record)
      record)))

(defn- create-roles 
  "Creates the roles that are to be made available globally if they do not exist."
  [def-org]
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

(defn- create-langs 
  "Creates the languages that are available from the base organization if they do not exist."
  [def-org]
  (doseq [^Locale lc (Locale/getAvailableLocales)]
    (create "language"
            {:name (.getDisplayName lc)
             :code (.getLanguage lc)
             :organization def-org})))

(defn- create-org []
  "Creates the base organziation if it does not exist."
  (create "organization"
          {:name "Default Organization"
           :code "deforg"}))

(defn- get-role-by-code
  "Looks up roles by there code value."
  [code]
  (first (data/find-records-by-attrs "role" {:code code})))

(defn- create-users 
  "Creates user that are required in the base organization if they do not exist.
   At a minimum this should be a Super Administrator"
  [def-org]
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

(defn seed-base-org! 
  "Creates the base organization that is used to define globally used data and the Super Administrators."
  []
  (let [def-org (create-org)]
    (create-roles def-org)
    (create-users def-org)
    (create-langs def-org)))

(defn seed-example-org!
  "Creates an organization that will contain all of the example and best practice data."
  []
  (let [org (create "organization" {:name "Example Organization" :code "example" :protocol-label "Form" :location-label "Division"})
        org-id (:id org)
        
        in-loc (create "location" {:name "In Patient" :code "inpatient" :organization org})
        out-loc (create "location" {:name "Out Patient" :code "outpatient" :organization org})
        sur-loc (create "location" {:name "Surgery" :code "surgery" :organization org})
        
        clerk (create "user" {:username "ex-collector" :password (auth/hash-password "password") :organization org 
                              :first-name "Example" :last-name "Clerk"})
        sclerk (create "user" {:username "ex-scollector" :password (auth/hash-password "password") :organization org
                                :first-name "Example" :last-name "SuperClerk"})
        admin (create "user" {:username "ex-admin" :password (auth/hash-password "password") :organization org
                               :first-name "Example" :last-name "Admin"})
        designer (create "user" {:username "ex-designer" :password (auth/hash-password "password") :organization org
                                  :first-name "Example" :last-name "Designer"})
        theman (create "user" {:username "ex-theman" :password (auth/hash-password "password") :organization org
                                :first-name "Example" :last-name "TheMan"})
        
        admin-grp (create "group" {:name "Example Admin Group" :code "ex-admins" :organization org})
        theman-grp (create "group" {:name "Example The Man Group" :code "ex-themans" :organization org})
        
        sadmin-role (get-role-by-code "sadmin")
        admin-role (get-role-by-code "admin")
        clerk-role (get-role-by-code "collect")
        manager-role (get-role-by-code "manage")
        designer-role (get-role-by-code "design")]
    
    ;; Create Admin Role Mappings
    (create "role-mapping" {:organization org :user admin :role admin-role})
    
    ;; Create Clerk Role Mappings
    (create "role-mapping" {:organization org :user clerk :role clerk-role :location in-loc})
    
    ;; Create Super Clerk Role Mappings
    (doseq [loc [in-loc out-loc sur-loc]]
      (create "role-mapping" {:organization org :user sclerk :role clerk-role :location loc}))
    
    ;; Create Protocol Designer Role Mappings
    (doseq [loc [in-loc out-loc sur-loc]]
      (create "role-mapping" {:organization org :user designer :role designer-role :location loc}))
    
    ;; Create Admin Group Role Mappings
    (create "role-mapping" {:organization org :group admin-grp :role admin-role})
    (data/relate-records "user" (:id admin) "group" (:id admin-grp))

    ;; Take Care Of The Man
    (data/relate-records "user" (:id theman) "group" (:id theman-grp))    
    (create "role-mapping" {:organization org :group theman-grp :role admin-role})
    (doseq [loc [in-loc out-loc sur-loc]]
      (create "role-mapping" {:organization org :group theman-grp :role clerk-role :location loc})
      (create "role-mapping" {:organization org :group theman-grp :role designer-role :location loc})
      (create "role-mapping" {:organization org :group theman-grp :role manager-role :location loc}))))

(defn reset-dev-db!
  []
  (do
    (println "Deleting all data...")
    (data/delete-all-nodes!)
    (println "Setting up schema...")
    (setup-default-schema!)
    (println "Seeding default data...")
    (seed-base-org!)
    (println "Seeding example data...")
    (seed-example-org!)
    "Done!"))
