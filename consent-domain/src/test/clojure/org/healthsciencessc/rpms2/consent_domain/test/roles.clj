(ns org.healthsciencessc.rpms2.consent-domain.test.roles
  (:use [org.healthsciencessc.rpms2.consent-domain.roles]
        [org.healthsciencessc.rpms2.consent-domain.types]
        [clojure.test]))

(def test-role-mappings
  [{:role {:id 1 :code code-role-superadmin }
    :organization {:id 2 :code "org"}
    :location {:id 3 :code "loc"}}
   {:role {:id 4 :code "rm"}
    :organization {:id 5 :code "org"}
    :location {:id 6 :code "loc"}}])

(def test-user
  {:role-mappings test-role-mappings 
   :name "Test User"})


(deftest test-role-checking
  (is (has-role? test-user {:id 4})
       "Falsely reported that a user was invalid")
  (is (has-role? test-user {:code code-role-superadmin }) "Falsely reported a superadmin as invalid")
  (is (has-role? test-user {:code code-role-superadmin } :organization {:id 2})
    "Falsely reported a user with constraints as invalid")
  (is (not (has-role? test-user {:code "sadfmin"}))
      "User named sadfmin is not valid!")
  (is (not (has-role? test-user {:code code-role-superadmin } :organization {:id 2}
           :location {:code "loc" :id 123}))
      "User with multiple constraint varargs isn't valid")
  (is (has-role? test-user {:code code-role-superadmin } :organization {:id 2}
           :location {:code "loc" })
      "User with multiple constraint varargs should be valid"))

(deftest test-superadmin
  (is (superadmin? test-user)
      "This user is actually a superadmin"))

