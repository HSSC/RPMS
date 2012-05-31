(ns org.healthsciencessc.rpms2.consent-admin.test.ui.common
  (:use [org.healthsciencessc.rpms2.consent-admin.ui.common])
  (:use [clojure.test]))

(deftest test-dotit
  (is (= (dotit "test") ".test"))
  (is (= (dotit ".test") ".test"))
  (is (= (dotit (name :test)) ".test"))
  (is (= (dotit (name :.test)) ".test")))

(deftest test-smooshkw
  (is (= :testdog (smooshkw :test "dog")))
  (is (not (= :testdog (smooshkw :test "cat"))))
  (is (= :testdog (smooshkw :test :dog)))
  (is (not (= :testdog (smooshkw :test :cat)))))

(deftest test-tag-class
  (is (= :test.dog (tag-class :test "dog")))
  (is (= :test.dog.cat.mouse (tag-class :test "dog" "cat" "mouse")))
  (is (= :test.dog.cat.mouse (tag-class :test :dog :cat :.mouse))))