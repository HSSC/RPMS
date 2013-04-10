;;Provides predicates that help determin if two of the same type match based on non identity metadata.
(ns org.healthsciencessc.consent.common.match)

(defn keys-exists?
  "Ensures that all of the keys exist in all of the maps."
  [keys & maps]
  (every? #(every? % maps) keys))

(defn keys-missing?
  "Ensures that all of the keys exist in all of the maps."
  [keys & maps]
  (every? #(not (every? % maps)) keys))

(defn deep-key-exists?
  "Ensures deep keys exist in map of maps."
  [keys & maps]
  (every? #(get-in % keys) maps))

(defn deep-key-missing?
  "Ensures deep keys exist in map of maps."
  [keys & maps]
  (every? #(not (get-in % keys)) maps))

(defn keys-match?
  "Ensures that values for al the keys match in all the maps."
  [keys & maps]
  (and (apply keys-exists? keys maps)
       (every? #(apply = (map % maps)) keys)))

(defn keys-match-or-missing?
  "Ensures that values for al the keys match in all the maps."
  [keys & maps]
  (or (apply keys-missing? keys maps)
      (and (apply keys-exists? keys maps)
           (every? #(apply = (map % maps)) keys))))

(defn deep-key-match?
  "Ensures values for deep keys match in map of maps."
  [keys & maps]
  (and (apply deep-key-exists? keys maps)
       (apply = (map #(get-in % keys) maps))))

(defn deep-key-match-or-missing?
  "Ensures values for deep keys match in map of maps."
  [keys & maps]
  (or (apply deep-key-missing? keys maps)
      (and (apply deep-key-exists? keys maps)
           (apply = (map #(get-in % keys) maps)))))

(defn same-org?
  "Determines if the organizations that are associated with the provided "
  [thing1 thing2]
  (deep-key-match? [:organization :id] thing1 thing2))

(defn codes-match?
  "Checks if two records have matching codes."
  [thing1 thing2]
  (keys-match? [:code] thing1 thing2))

(defn names-match?
  "Checks if two records have matching codes."
  [thing1 thing2]
  (keys-match? [:name] thing1 thing2))

(defn orgs-codes-match?
  "Checks if two records match each other based on the 
   value of the code within the same organization."
  [thing1 thing2]
  (and (same-org? thing1 thing2)
    (codes-match? thing1 thing2)))

(defn orgs-names-match?
  "Checks if two records match each other based on the 
   value of the name within the same organization."
  [thing1 thing2]
  (and (same-org? thing1 thing2)
    (names-match? thing1 thing2)))

(defn users-match?
  "Checks if two roles match each other based on rules for metadata uniqueness, which is based on the value of
   the username."
  [thing1 thing2]
  (keys-match? [:username] thing1 thing2))

(defn orgs-match?
  "Checks if two organizations match each other based on rules for metadata uniqueness, which is based on the 
   value of the code."
  [thing1 thing2]
  (codes-match? thing1 thing2))

(defn roles-match?
  "Checks if two roles match each other based on rules for metadata uniqueness, which is based on the 
   value of the code."
  [thing1 thing2]
  (codes-match? thing1 thing2))

(defn locations-match?
  "Checks if two locations match each other based on rules for metadata uniqueness, which is based on the 
   value of the code within the same organization."
  [thing1 thing2]
  (orgs-codes-match? thing1 thing2))

(defn groups-match?
  "Checks if two groups match each other based on rules for metadata uniqueness, which is based on the 
   value of the code within the same organization."
  [thing1 thing2]
  (orgs-names-match? thing1 thing2))

(defn role-mapping-match?
  "Checks if two role-mappings match each other based on rules for metadata uniqueness, which is based on the 
   value of the code within the same organization."
  [thing1 thing2]
  (and (same-org? thing1 thing2)
       (deep-key-match? [:role :id] thing1 thing2)
       (deep-key-match-or-missing? [:location :id] thing1 thing2)
       (deep-key-match-or-missing? [:user :id] thing1 thing2)
       (deep-key-match-or-missing? [:group :id]thing1 thing2)))
