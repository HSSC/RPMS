(ns org.healthsciencessc.rpms2.consent-services.test.core
  (:use [org.healthsciencessc.rpms2.consent-services.core]
        [clojure.test])
  (:require [org.healthsciencessc.rpms2.consent-services.process :as process])
  (:import [org.healthsciencessc.rpms2.consent_services.process DefaultProcess]))

(defn test-request [resource routes method & params]
  (routes {:request-method method :uri resource :params (first params)}))

(deftest test-uri-process-convertion
  (is (= "get-foo-bar" (uri->process-name "get" "/foo/bar")))
  (is (= "post-baz-quz" (uri->process-name "post" "/baz/quz"))))

(deftest test-404-for-no-process
  (let [response (test-request "/foo/bar" service-routes :get)]
    (is (= 404 (:status response)))))

