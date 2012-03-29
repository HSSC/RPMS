(ns org.healthsciencessc.rpms2.consent-domain.test.core
  (:use [org.healthsciencessc.rpms2.consent-domain.core])
  (:use [clojure.test]))

(deftest get-required-attrs-from-data-def
  (is (= (hash-set :foo :baz)
         (set (required-attrs {:attributes {:foo {:required true}
                                                :bar nil
                                            :baz {:required true}}})))))

(deftest get-persisted-attrs-from-data-def
  (is (= (hash-set :foo :bar)
         (set (persisted-attrs {:attributes {:foo {:persisted true}
                                                :bar {:persisted true}
                                             :baz nil}})))))

(deftest get-relationship-for-parent-from-child
  (let [data-defs {"foo" {:relations [{:type :has-many :related-to "bar"}]}
                   "bar" {:relations [{:type :belongs-to :related-to "foo" :relationship :foobar}]}}]
    (is (= :foobar
           (get-relationship-from-child "foo" "bar" data-defs)))))