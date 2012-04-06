(ns org.healthsciencessc.rpms2.process-engine.test.path
  (:use [org.healthsciencessc.rpms2.process-engine.path]
        [clojure.test]))

(deftest test-rootify
  (is (= "/base" (rootify "base")))
  (is (= "/base" (rootify "base/")))
  (is (= "/base" (rootify "/base")))
  (is (= "/" (rootify "/")))
  (is (empty? (rootify nil))))

(deftest test-root-link
  (is (= "/base/last" (root-link "base" "last")))
  (is (= "/base/last" (root-link "base" "/last")))
  (is (= "/base/last" (root-link "base" "last/")))
  (is (= "/base/last" (root-link "base" "/last/")))
  (is (= "/base/last" (root-link "/base" "last/")))
  (is (= "/base/last" (root-link "base/" "last/")))
  (is (= "/base/last" (root-link "/base/" "last/")))
  (is (= "/last" (root-link "/" "last/")))
  (is (= "/last" (root-link nil "last/")))
  (let [parms0 {:context "/base" :path-info "/"}
        parms1 {:context "/base" :path-info "/path"}
        parms2 {:context "/base" :path-info "/path/end"}
        parmsx {:context nil :path-info "/path"}
        parmsy {:context "/base" :path-info nil}]
    (is (= "/base/last" (root-link parms0 "/last")))
    (is (= "/base/last" (root-link parms1 "/last")))
    (is (= "/base/last" (root-link parms2 "/last")))
    (is (= "/base/last" (root-link parms0 "last")))
    (is (= "/base/last" (root-link parms1 "last")))
    (is (= "/base/path/last" (root-link parms2 "last")))
    (is (= "/last" (root-link parmsx "last")))
    (is (= "/last" (root-link parmsx "/last")))
    (is (= "/base/last" (root-link parmsy "last")))
    (is (= "/base/last" (root-link parmsy "/last")))
  ))