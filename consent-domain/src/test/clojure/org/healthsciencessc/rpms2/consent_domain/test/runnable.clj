(ns org.healthsciencessc.rpms2.consent-domain.test.runnable
  (:require [org.healthsciencessc.rpms2.consent-domain.test.roles :as roles])
  (:use [org.healthsciencessc.rpms2.consent-domain.runnable]
        [org.healthsciencessc.rpms2.consent-domain.lookup]
        [clojure.test]))

(defn get-test-super
  [_]
  roles/test-super)

(defn get-test-admin
  [_]
  roles/test-admin)

(defn get-test-designer
  [_]
  roles/test-designer)

(defn get-test-collector
  [_]
  roles/test-collector)

(deftest test-gen-collector-location-check
  (let [designerfn (gen-collector-location-check get-test-designer get-location-in-query)
        collectorfn (gen-collector-location-check get-test-collector get-location-in-query)
        ctx {:query-params {:location (:id roles/test-loc)}}
        badctx {:query-params {:location "NOTAGUID"}}]
    (is (collectorfn ctx))
    (is (not (collectorfn badctx)))
    (is (not (designerfn ctx)))))

(deftest test-gen-designer-location-check
  (let [designerfn (gen-designer-location-check get-test-designer get-location-in-query)
        collectorfn (gen-designer-location-check get-test-collector get-location-in-query)
        ctx {:query-params {:location (:id roles/test-loc)}}
        badctx {:query-params {:location "NOTAGUID"}}]
    (is (designerfn ctx))
    (is (not (designerfn badctx)))
    (is (not (collectorfn ctx)))))

(deftest test-super-or-admin
  (let [superfn (gen-super-or-admin get-test-super)
        adminfn (gen-super-or-admin get-test-admin)
        designerfn (gen-super-or-admin get-test-designer)
        collectorfn (gen-super-or-admin get-test-collector)
        ctx {}]
    (is (superfn ctx))
    (is (adminfn ctx))
    (is (not (designerfn ctx)))
    (is (not (collectorfn ctx)))))

(deftest test-super-or-admin-by-org
  (let [superfn (gen-super-or-admin-by-org get-test-super get-organization-in-query)
        adminfn (gen-super-or-admin-by-org get-test-admin get-organization-in-query)
        ctx {:query-params {:organization (:id roles/test-org)}}
        basectx {:query-params {:organization (:id roles/test-baseorg)}}]
    (is (superfn {}))
    (is (superfn ctx))
    (is (superfn basectx))
    (is (adminfn {}))
    (is (adminfn ctx))
    (is (not (adminfn basectx)))))

(defn org-record-from-query
  [ctx]
  (let [org-id (get-organization-in-query ctx)]
    {:organization {:id org-id}}))

(deftest test-gen-super-or-admin-record-check
  (let [superfn (gen-super-or-admin-record-check get-test-super org-record-from-query)
        adminfn (gen-super-or-admin-record-check get-test-admin org-record-from-query)
        ctx {:query-params {:organization (:id roles/test-org)}}
        basectx {:query-params {:organization (:id roles/test-baseorg)}}]
    (is (superfn {}))
    (is (superfn ctx))
    (is (superfn basectx))
    (is (not (adminfn {})))
    (is (adminfn ctx))
    (is (not (adminfn basectx)))))


(deftest test-gen-admin-record-check
  (let [adminfn (gen-admin-record-check get-test-admin org-record-from-query)
        ctx {:query-params {:organization (:id roles/test-org)}}
        basectx {:query-params {:organization (:id roles/test-baseorg)}}]
    (is (not (adminfn {})))
    (is (adminfn ctx))
    (is (not (adminfn basectx)))))
