(ns org.healthsciencessc.rpms2.consent-domain.test.core
  (:use [org.healthsciencessc.rpms2.consent-domain.core])
  (:use [clojure.test]))


(def test-data-defs
  {"foo-type" {:attributes {:foo {:required true}
                             :bar nil
                            :baz {:required true :persisted true}}
               :relations [{:type :has-many :related-to "bar-type"}]}
   "bar-type" {:relations [{:type :belongs-to :related-to "foo-type" :relationship :foobar}]}})

(deftest get-required-attrs-from-data-def
  (is (= (hash-set :foo :baz)
         (set (get-attrs "foo-type" test-data-defs :required)))))

(deftest get-persisted-attrs-from-data-def
  (is (= (hash-set :baz)
         (set (get-attrs "foo-type" test-data-defs :persisted)))))

(deftest get-relationship-for-parent-from-child
  (is (= :foobar
         (get-relationship-from-child "foo-type" "bar-type" test-data-defs))))

(deftest omit-attributes
  (let [attr-map {:attributes {:foo {:persisted true}
                               :bar {:omit true}}}]
    (is (= '(:foo) (all-valid-keys attr-map)))))