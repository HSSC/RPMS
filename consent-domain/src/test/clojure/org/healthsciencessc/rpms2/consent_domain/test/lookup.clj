(ns org.healthsciencessc.rpms2.consent-domain.test.lookup
  (:use [org.healthsciencessc.rpms2.consent-domain.lookup])
  (:use [clojure.test]))


(deftest test-gen-request-lookup 
  (let [lookup (gen-request-lookup [:here :id])]
    (is (lookup {:here {:id true}}))
    (is (not (lookup {:here {:id false}})))
    (is (not (lookup {:here {:yo true}})))))

(deftest test-gen-request-query-lookup
  (let [lookup (gen-request-query-lookup [:here :id])]
    (is (lookup {:query-params {:here {:id true}}}))
    (is (not (lookup {:query-params {:here {:id false}}})))
    (is (not (lookup {:here {:yo true}})))))

(deftest test-gen-request-body-lookup
  (let [lookup (gen-request-body-lookup [:here :id])]
    (is (lookup {:body-params {:here {:id true}}}))
    (is (not (lookup {:body-params {:here {:id false}}})))
    (is (not (lookup {:here {:yo true}})))))


;; Commonly Used Generated Lookup Functions For Types
;; Lookups the search for the ID of the type in the request params using the common naming pattern.
(deftest test-get-endorsement-in-query 
  (is (get-endorsement-in-query {:query-params {:endorsement true}}))
  (is (not (get-endorsement-in-query {:query-params {:whatever true}}))))

(deftest test-get-endorsement-type-in-query
  (is (get-endorsement-type-in-query {:query-params {:endorsement-type true}}))
  (is (not (get-endorsement-type-in-query {:query-params {:whatever true}}))))

(deftest test-get-group-in-query
  (is (get-group-in-query {:query-params {:group true}}))
  (is (not (get-group-in-query {:query-params {:whatever true}}))))
                                                
(deftest test-get-language-in-query
  (is (get-language-in-query {:query-params {:language true}}))
  (is (not (get-language-in-query {:query-params {:whatever true}}))))
                                                
(deftest test-get-location-in-query
  (is (get-location-in-query {:query-params {:location true}}))
  (is (not (get-location-in-query {:query-params {:whatever true}}))))

(deftest test-get-meta-item-in-query
  (is (get-meta-item-in-query {:query-params {:meta-item true}}))
  (is (not (get-meta-item-in-query {:query-params {:whatever true}}))))

(deftest test-get-organization-in-query
  (is (get-organization-in-query {:query-params {:organization true}}))
  (is (not (get-organization-in-query {:query-params {:whatever true}}))))

(deftest test-get-policy-in-query
  (is (get-policy-in-query {:query-params {:policy true}}))
  (is (not (get-policy-in-query {:query-params {:whatever true}}))))

(deftest test-get-protocol-in-query
  (is (get-protocol-in-query {:query-params {:protocol true}}))
  (is (not (get-protocol-in-query {:query-params {:whatever true}}))))

(deftest test-get-protocol-version-in-query
  (is (get-protocol-version-in-query {:query-params {:protocol-version true}}))
  (is (not (get-protocol-version-in-query {:query-params {:whatever true}}))))

(deftest test-get-role-in-query
  (is (get-role-in-query {:query-params {:role true}}))
  (is (not (get-role-in-query {:query-params {:whatever true}}))))

(deftest test-get-role-mapping-in-query
  (is (get-role-mapping-in-query {:query-params {:role-mapping true}}))
  (is (not (get-role-mapping-in-query {:query-params {:whatever true}}))))

(deftest test-get-user-in-query
  (is (get-user-in-query {:query-params {:user true}}))
  (is (not (get-user-in-query {:query-params {:whatever true}}))))

(deftest test-get-widget-in-query
  (is (get-widget-in-query {:query-params {:widget true}}))
  (is (not (get-widget-in-query {:query-params {:whatever true}}))))


(deftest test-get-organization-in-body
  (is (get-organization-in-body {:body-params {:organization {:id true}}}))
  (is (not (get-organization-in-body {:body-params {:whatever true}}))))


(def context {:query-params {:organization 1}
              :body-params {:organization {:id 2}}
              :org 3})

(deftest test-gen-organization-lookup 
  (let [lookup (gen-organization-lookup #(:org %))]
    (is (= 1 (lookup context)))
    (is (= 2 (lookup (dissoc context :query-params)))
    (is (= 3 (lookup (dissoc context :query-params :body-params)))))))

(deftest test-gen-organization-lookup-in-query 
  (let [lookup (gen-organization-lookup-in-query #(:org %))]
    (is (= 1 (lookup context)))
    (is (= 3 (lookup (dissoc context :query-params)))
    (is (= 3 (lookup (dissoc context :query-params :body-params)))))))

(deftest test-gen-organization-lookup-in-body 
  (let [lookup (gen-organization-lookup-in-body #(:org %))]
    (is (= 2 (lookup context)))
    (is (= 2 (lookup (dissoc context :query-params)))
    (is (= 3 (lookup (dissoc context :query-params :body-params)))))))
