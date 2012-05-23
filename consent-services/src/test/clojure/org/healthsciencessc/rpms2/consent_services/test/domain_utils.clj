(ns org.healthsciencessc.rpms2.consent-services.test.domain-utils
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils]
        [org.healthsciencessc.rpms2.consent-domain.types]
        [clojure.test]))

(def test-role-mappings
  [{:location {:name "Test"} :role {:name "Admin" :code code-role-admin} :organization {:name "Foo"}}
   {:location {:name "Test2"} :role {:name "Super Admin" :code code-role-superadmin } :organization {:name "Foo"}}])

(deftest finds-code-in-codes-coll
  (is (code-in-codes? "foo" '("bar" "foo" "baz"))))

(deftest doesnt-find-code-in-coll
  (is (not (code-in-codes? "foo" '("bar" "baz")))))

(deftest finds-role-codes
  (let [role-codes (get-role-codes test-role-mappings)]
    (is (= 2 (count role-codes)))
    (is (code-in-codes? code-role-admin role-codes))
    (is (code-in-codes? code-role-superadmin role-codes))))

(deftest is-super-admin
  (is (super-admin? {:username "foo" :role-mappings test-role-mappings})))

(deftest is-admin
  (is (admin? {:username "foo" :role-mappings test-role-mappings})))

(deftest is-not-super-admin
  (is (not (super-admin? {:username "foo" :role-mappings (vec (nth test-role-mappings 0))}))))

(deftest is-not-admin
  (is (not (admin? {:username "foo" :role-mappings (vec (nth test-role-mappings 1))}))))
