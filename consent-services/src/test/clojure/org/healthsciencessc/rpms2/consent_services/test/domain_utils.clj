(ns org.healthsciencessc.rpms2.consent-services.test.domain-utils
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils]
        [clojure.test]))

(def test-role-mappings
  [{:location {:name "Test"} :role {:name "Admin" :code "admin"} :organization {:name "Foo"}}
   {:location {:name "Test2"} :role {:name "Super Admin" :code "sadmin"} :organization {:name "Foo"}}])

(deftest finds-code-in-codes-coll
  (is (code-in-codes? "foo" '("bar" "foo" "baz"))))

(deftest doesnt-find-code-in-coll
  (is (not (code-in-codes? "foo" '("bar" "baz")))))

(deftest finds-role-codes
  (let [role-codes (get-role-codes test-role-mappings)]
    (is (= 2 (count role-codes)))
    (is (code-in-codes? "admin" role-codes))
    (is (code-in-codes? "sadmin" role-codes))))

(deftest is-super-admin
  (is (super-admin? {:username "foo" :role-mappings test-role-mappings})))

(deftest is-admin
  (is (admin? {:username "foo" :role-mappings test-role-mappings})))

(deftest is-not-super-admin
  (is (not (super-admin? {:username "foo" :role-mappings (vec (nth test-role-mappings 0))}))))

(deftest is-not-admin
  (is (not (admin? {:username "foo" :role-mappings (vec (nth test-role-mappings 1))}))))
