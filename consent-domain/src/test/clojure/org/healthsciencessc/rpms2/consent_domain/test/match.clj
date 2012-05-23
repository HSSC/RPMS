(ns org.healthsciencessc.rpms2.consent-domain.test.match
  (:use [org.healthsciencessc.rpms2.consent-domain.match]
        [clojure.test]))

(def test-code1-org1-dup
  {:code "code1" 
   :name "Code 1"
   :organization {:id 1}
   :random "CARING?"})

(def test-code1-org1
  {:code "code1" 
   :name "Code 1"
   :organization {:id 1}
   :random "YOU CARE?"})

(def test-code1-org2
  {:code "code1"  
   :name "Code 1"
   :organization {:id 2}
   :random "BOO CARES!"})

(def test-code2-org1
  {:code "code2"  
   :name "Code 2"
   :organization {:id 1}
   :random "WHO CARES?"})

(def test-code2-org2
  {:code "code2"  
   :name "Code 2"
   :organization {:id 2}
   :random "WHO CARES!"})

(deftest test-keys-exists
  (is (keys-exists? [:code :name] test-code1-org1 {:code "n" :name "n" :test :test})
      "keys-exists? did not evaluate a true scenario.")
  (is (not (keys-exists? [:code :name] {:code "n" :name "n"} {:code "n" :test :test}))
      "keys-exists? did not evaluate a false scenario."))

(deftest test-deep-key-exists
  (is (deep-key-exists? [:organization :id] test-code2-org1 test-code2-org1)
      "deep-key-exists? not evaluate a true scenario.")
  (is (not (deep-key-exists? [:organization :id] test-code2-org1 {}))
      "deep-key-exists? not evaluate a false scenario."))

(deftest test-keys-match
  (is (keys-match? [:code :name] test-code1-org1 test-code1-org2)
      "keys-match? did not evaluate a true scenario.")
  (is (not (keys-match? [:code :name] test-code1-org1 test-code2-org2))
      "keys-match? did not evaluate a false scenario."))

(deftest test-deep-key-match
  (is (deep-key-match? [:organization :id] test-code1-org1 test-code2-org1)
      "deep-key-match? did not evaluate a true scenario.")
  (is (not (deep-key-match? [:organization :id] test-code1-org1 test-code2-org2))
      "deep-key-match? did not evaluate a false scenario."))

(deftest test-users-match
  (is (users-match? {:username "BOB" :password "NOPE"} {:username "BOB" :password "YEP"})
       "User match not matching correctly.")
  (is (not (users-match? {:username "JIM" :password "NOPE"} {:username "BOB" :password "YEP"}))
       "User non-match not matching correctly."))

(deftest test-orgs-match
  (is (orgs-match? test-code1-org1 test-code1-org2)
       "Organization match not matching correctly.")
  (is (not (orgs-match? test-code1-org1 test-code2-org2))
       "Organization non-match not matching correctly."))

(deftest test-roles-match
  (is (roles-match? test-code1-org1 test-code1-org2)
       "Roles match not matching correctly.")
  (is (not (roles-match? test-code1-org1 test-code2-org1))
       "Roles non-match not matching correctly."))

(deftest test-locations-match
  (is (locations-match? test-code1-org1 test-code1-org1-dup)
       "Locations match not matching correctly.")
  (is (not (locations-match? test-code1-org1 test-code2-org1))
       "Locations non-match not matching correctly.")
  (is (not (locations-match? test-code1-org1 test-code1-org2))
       "Locations non-match not matching correctly.")
  (is (not (locations-match? test-code1-org1 test-code2-org2))
       "Locations non-match not matching correctly."))

(deftest test-groups-match
  (is (groups-match? test-code1-org1 test-code1-org1-dup)
       "Groups match not matching correctly.")
  (is (not (groups-match? test-code1-org1 test-code1-org2))
       "Groups non-match not matching correctly.")
  (is (not (groups-match? test-code1-org1 test-code2-org1))
       "Groups non-match not matching correctly.")
  (is (not (groups-match? test-code1-org1 test-code2-org2))
       "Groups non-match not matching correctly."))
