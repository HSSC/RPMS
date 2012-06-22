(ns org.healthsciencessc.rpms2.consent-services.seed
  (:require [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-services.auth :as auth]
            [org.healthsciencessc.rpms2.consent-domain.core :as domain]
            [org.healthsciencessc.rpms2.consent-domain.match :as match]
            [org.healthsciencessc.rpms2.consent-domain.types :as types])
  (:use [org.healthsciencessc.rpms2.consent-domain.types])
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
  [type props matcher]
  (let [cache (get-cache type)]
    (some #(if (matcher props %) %) cache)))

(defn update-cache 
  "Puts a type record in the seed cache after it has been created."
  [type record]
  (swap! db-cache update-in [type] conj record))

(defn replace-in-cache 
  "Replaces an existing object with a new an improved object."
  [type record]
  (let [cache (get-cache type)
        lesscache (remove #(= (:id record) (:id %)) cache)
        newcache (conj lesscache record)]
  (swap! db-cache assoc type newcache)))

(defn create
  "Makes or finds a type in the database.  Allows for the seeding of data on startup without the risk of duplication."
  ([type props] (create type props #(= (bare-record %1) (bare-record %2))))
  ([type props matcher]
    (if-let [record (exists-in-db? type props matcher)]
      record
      (let [record (data/create type props)]
        (update-cache type record)
        record))))

(defn create-role-mapping
  [type mapping]
  (let [owner (type mapping)
        roles (:role-mappings owner)
        match (some #(if (match/role-mapping-match? mapping (assoc % type owner)) %) roles)]
    (if match
      match
      (let [data (data/create role-mapping mapping)
            newroles (conj roles data)
            newowner (assoc owner :role-mappings newroles)]
        (replace-in-cache (name type) newowner)
        data))))
  

(defn- create-roles 
  "Creates the roles that are to be made available globally if they do not exist."
  [def-org]
  (doseq [[name code reqloc] [["Administrator" code-role-admin false]
                       ["Consent Collector" code-role-collector true]
                       ["Consent Designer" code-role-designer true]
                       ["Consent Manager" code-role-consentmanager true]
                       ["Consent System" code-role-externalsystem false]]]
    (create "role" {:name name
                    :code code
                    :requires-location reqloc
                    :organization def-org} match/roles-match?)))

(defn- create-default-lang
  "Creates the languages that are available from the base organization if they do not exist."
  [def-org]
  (create "language"
            {:name "English"
             :code "en"
             :organization def-org}))
(defn- create-langs 
  "Creates the languages that are available from the base organization if they do not exist."
  [def-org]
  (doseq [[code name] [["es" "Spanish"]
                       ["fr" "French"]
                       ["de" "German"]]]
    (create "language"
            {:name name
             :code code
             :organization def-org})))

(defn- create-endorsement-types 
  "Creates the languages that are available from the base organization if they do not exist."
  [def-org]
  (doseq [[name code] [["Primary Witness Signature" types/code-endorsement-type-witness]
                       ["Secondary Witness Signature" types/code-endorsement-type-witness2]
                       ["Consenter Signature" "!consenter.signature!"]
                       ["Consenter Initials" "!consenter.initials!"]
                       ["Guardian Signature" "!guardian.signature!"]
                       ["Guardian Initials" "!guardian.initials!"]
                       ["Guarantor Signature" "!guarantor.signature!"]
                       ["Guarantor Initials" "!guarantor.initials!"]]]
    (create types/endorsement-type
            {:name name
             :code code
             :status types/status-published
             :organization def-org})))

(defn- create-meta-items 
  "Creates the languages that are available from the base organization if they do not exist."
  [org lang]
  (doseq [[name datatype] 
          [["Signatory" "string"]
           ["Signatory's Name" "string"]
           ["Signatory's Relationship" "string"]
           ["Guarantor" "string"]
           ["Guarantor's Name" "string"]
           ["Referring Physician's Name" "string"]
           ["Referring Physician's City" "string"]
           ["Primary Care Physician's Name" "string"]
           ["Primary Care Physician's City" "string"]
           ["Attending Physician's Name" "string"]
           ["Advanced Directives Given" "boolean"]]]
    (create types/meta-item
            {:name name
             :data-type datatype
             :label {:value name :language lang}
             :status types/status-published
             :organization org})))

(defn- create-policy-definitions 
  "Creates the policy definitions that are available from the base organization if they do not exist."
  [org]
  (doseq [[name code description] 
          [["Purpose" "purpose" "A statement that the study involves research, an explanation of the purposes of the research and the expected duration of the subject's participation, a description of the procedures to be followed, and identification of any procedures which are experimental."]
           ["Risks" "risks" "A description of any reasonably foreseeable risks or discomforts to the subject."]
           ["Benefits" "benefits" "A description of any benefits to the subject or to others which may reasonably be expected from the research."]
           ["Alternative Procedures" "alternative-procedures" "A disclosure of appropriate alternative procedures or courses of treatment, if any, that might be advantageous to the subject."]
           ["Confidentiality" "confidentiality" "A statement describing the extent, if any, to which confidentiality of records identifying the subject will be maintained."]
           ["Compensation" "compensation" "For research involving more than minimal risk, an explanation as to whether any compensation and an explanation as to whether any medical treatments are available if injury occurs and, if so, what they consist of, or where further information may be obtained."]
           ["Contact Procedure" "contact-procedure" "An explanation of whom to contact for answers to pertinent questions about the research and research subjects' rights, and whom to contact in the event of a research-related injury to the subject."]
           ["Voluntary Participation" "voluntary-participation" "A statement that participation is voluntary, refusal to participate will involve no penalty or loss of benefits to which the subject is otherwise entitled, and the subject may discontinue participation at any time without penalty or loss of benefits to which the subject is otherwise entitled."]
           ["Unforeseeable Risks" "unforeseeable-risks" "A statement that the particular treatment or procedure may involve risks to the subject (or to the embryo or fetus, if the subject is or may become pregnant) which are currently unforeseeable."]
           ["Participant Termination" "participant-termination" "Anticipated circumstances under which the subject's participation may be terminated by the investigator without regard to the subject's consent."]
           ["Costs" "costs" "Any additional costs to the subject that may result from participation in the research."]
           ["Participant Withdrawal" "participant-withdrawal" "The consequences of a subject's decision to withdraw from the research and procedures for orderly termination of participation by the subject"]
           ["Research Findings" "research-findings" "A statement that significant new findings developed during the course of the research which may relate to the subject's willingness to continue participation will be provided to the subject."]
           ["Number Of Subjects" "number-of-subjects" "The approximate number of subjects involved in the study."]]]
    (create types/policy-definition
            {:name name
             :description description
             :code code
             :organization org})))

(defn- get-role-by-code
  "Looks up roles by there code value."
  [code]
  (first (data/find-records-by-attrs role {:code code})))

(defn seed-base-org! 
  "Creates the base organization that is used to define globally used data and the Super Administrators."
  []
  (let [org (create organization {:name "Default Organization" :code code-base-org} match/orgs-match?)
        sadmin-role (create "role" 
                            {:name "Super Administrator" :code code-role-superadmin :organization org :requires-location false} 
                            match/roles-match?)
        sadmin (create user 
                       {:first-name "Super" :last-name "Administrator" :organization org
                        :username "admin" :password (auth/hash-password "root")} 
                       match/users-match?)
        lang (create-default-lang org)]
    
    (create-role-mapping :user {:organization org :role sadmin-role :user sadmin})
    (create-roles org)
    (create-langs org)
    (create-policy-definitions org)
    (create-meta-items org lang)
    (create-endorsement-types org)))

(defn seed-example-org!
  "Creates an organization that will contain all of the example and best practice data."
  []
  (let [org (create organization 
                    {:name "Example Organization" :code "example" :protocol-label "Form" :location-label "Division"}
                    match/orgs-match?)
        org-id (:id org)
        
        in-loc (create location {:name "In Patient" :code "inpatient" :organization org} match/locations-match?)
        out-loc (create location {:name "Out Patient" :code "outpatient" :organization org} match/locations-match?)
        sur-loc (create location {:name "Surgery" :code "surgery" :organization org} match/locations-match?)
        
        clerk (create user {:username "ex-collector" :password (auth/hash-password "password") :organization org 
                              :first-name "Example" :last-name "Clerk"} match/users-match?)
        sclerk (create user {:username "ex-scollector" :password (auth/hash-password "password") :organization org
                                :first-name "Example" :last-name "SuperClerk"} match/users-match?)
        admin (create user {:username "ex-admin" :password (auth/hash-password "password") :organization org
                               :first-name "Example" :last-name "Admin"} match/users-match?)
        designer (create user {:username "ex-designer" :password (auth/hash-password "password") :organization org
                                  :first-name "Example" :last-name "Designer"} match/users-match?)
        theman (create user {:username "ex-theman" :password (auth/hash-password "password") :organization org
                                :first-name "Example" :last-name "TheMan"} match/users-match?)
        
        admin-grp (create group {:name "Example Admin Group" :organization org} match/groups-match?)
        theman-grp (create group {:name "Example The Man Group" :organization org} match/groups-match?)
        
        sadmin-role (get-role-by-code code-role-superadmin)
        admin-role (get-role-by-code code-role-admin)
        clerk-role (get-role-by-code code-role-collector)
        manager-role (get-role-by-code code-role-consentmanager)
        designer-role (get-role-by-code code-role-designer)]
    
    ;; Create Admin Role Mappings
    (create-role-mapping :user {:organization org :user admin :role admin-role})
    
    ;; Create Clerk Role Mappings
    (create-role-mapping :user {:organization org :user clerk :role clerk-role :location in-loc})
    
    ;; Create Super Clerk Role Mappings
    (doseq [loc [in-loc out-loc sur-loc]]
      (create-role-mapping :user {:organization org :user sclerk :role clerk-role :location loc}))
    
    ;; Create Protocol Designer Role Mappings
    (doseq [loc [in-loc out-loc sur-loc]]
      (create-role-mapping :user {:organization org :user designer :role designer-role :location loc}))
    
    ;; Create Admin Group Role Mappings
    (create-role-mapping :group {:organization org :group admin-grp :role admin-role})
    (data/relate-records "user" (:id admin) "group" (:id admin-grp))

    ;; Take Care Of The Man
    (data/relate-records "user" (:id theman) "group" (:id theman-grp))    
    (create-role-mapping :group {:organization org :group theman-grp :role admin-role})
    (doseq [loc [in-loc out-loc sur-loc]]
      (create-role-mapping :group {:organization org :group theman-grp :role clerk-role :location loc})
      (create-role-mapping :group {:organization org :group theman-grp :role designer-role :location loc})
      (create-role-mapping :group {:organization org :group theman-grp :role manager-role :location loc}))))


(defn seed
  "Performs whatever seeding will need to take place."
  []
  (setup-default-schema!)
  (seed-base-org!)
  (seed-example-org!))

(defn reset-dev-db!
  []
  (do
    (println "Deleting all data...")
    (data/delete-all-nodes!)
    (println "Seeding...")
    (seed)
    "Done!"))
