(ns org.healthsciencessc.rpms2.consent-services.test.core
  (:use [org.healthsciencessc.rpms2.consent-services.core])
  (:use [clojure.test]))

(defn test-request [resource routes method & params]
  (routes {:request-method method :uri resource :params (first params)}))

(deftest test-route
  (is (= 404 (:status (test-request "/security/users" service-routes :get)))))

