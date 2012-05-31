(ns org.healthsciencessc.rpms2.consent-domain.test.runnable
  (:require [org.healthsciencessc.rpms2.consent-domain.test.roles :as roles])
  (:use [org.healthsciencessc.rpms2.consent-domain.runnable]
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
  (let [designerfn (gen-collector-location-check get-test-designer)
        collectorfn (gen-collector-location-check get-test-collector)
        ctx {:query-params {:location (:id roles/test-loc)}}
        badctx {:query-params {:location "NOTAGUID"}}]
    (is (collectorfn ctx))
    (is (not (collectorfn badctx)))
    (is (not (designerfn ctx)))))

(deftest test-gen-designer-location-check
  (let [designerfn (gen-designer-location-check get-test-designer)
        collectorfn (gen-designer-location-check get-test-collector)
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
  (let [superfn (gen-super-or-admin-by-org get-test-super)
        adminfn (gen-super-or-admin-by-org get-test-admin)
        ctx {:query-params {:organization (:id roles/test-org)}}
        basectx {:query-params {:organization (:id roles/test-baseorg)}}]
    (is (superfn {}))
    (is (superfn ctx))
    (is (superfn basectx))
    (is (adminfn {}))
    (is (adminfn ctx))
    (is (not (adminfn basectx)))))
    