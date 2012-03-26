(ns org.healthsciencessc.rpms2.consent-collector.test.core
  (:require [org.healthsciencessc.rpms2.process-engine.core :as pe]
            [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
            [org.healthsciencessc.rpms2.consent-collector.test.factories :as factories])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]])
  (:use [sandbar.stateful-session :only (session-get)])
  (:use org.healthsciencessc.rpms2.consent-collector.core
        [clojure.data.json :only (read-json json-str)]
        org.healthsciencessc.rpms2.consent-collector.test.helpers)
  (:use clojure.test))

(defn redirects?
  "Returns true if the response map is a redirect to the
  given location."
  [resp location]
  (and
   (map? resp)
   (= (resp :status) 302)
   (= (get-in resp [:headers "Location"] ) location )))

(def-rpms-test get-login-test
  "Test that login maps to view/login, with a 302 status."
  (is (redirects? (default-get-login {}) "/view/login")))

(def-rpms-test get-view-login-test
  "Test that an HTML login page is returned."
  (let [html (default-get-view-login {})]
    (is (re-find #"<input[^>]*password.*>" html))))

(def-rpms-test post-view-login-test
  "Test authentication."
  (are [doc status location]
    (testing doc
      (with-redefs [dsa/authenticate
                    (constantly {:status status})]
        (let [resp (default-post-view-login {:userid "foobar", :password "hunter2"})]
          (is (redirects? resp location)))))
    "Authentication succeeds" 200 "/view/select/location"
    "Authentication fails" 401 "/view/login"
    "User doesn't exist" 404 "/view/login"))


(def-rpms-test get-view-select-location-test
  (are [doc locations path-or-regex]
       (testing doc
         (with-session {:user (apply factories/user-with-locations locations)}
           (-> (default-get-view-select-location {})
               ((fn [resp]
                  (if (string? path-or-regex)
                    (redirects? resp path-or-regex)
                    (re-find path-or-regex resp))))
               (is))))
       "No authorized locations" [] "/view/not-authorized"
       "One location" ["Hardy har"] "/view/select/lock-code"
       "Many locations" ["foo" "bar" "baz"] #"foo"))

(def-rpms-test get-view-not-authorized-test
  (let [msg (i18n :not-authorized-message)]
    (is msg)
    (->> (default-get-view-not-authorized {})
         (re-find (re-pattern msg))
         (is))))

(def-rpms-test authorized-locations-test
  (let [user (factories/user-with-roles-and-locations
               ["Consent Collector" "foo"
                "Party Thrower"     "bar"
                "Consent Collector" "baz"])
        locs (authorized-locations user)]
    (= #{"foo" "baz"} (->> locs (map :name) set))))

;;======================================
;; Test Select Lock Code 
;; Cases: 
;; 1. the html form
;; 2. the submitted html form, with valid 4 digit lockcode
;; 3. the submitted html form, with invalid lockcode
;; 4. the submitted html form, with no lockcode
;; will go to /view/select/consenter
(def-rpms-test get-view-select-lockcode
  "Test that get-view-select-lockcode displays a valid input form for entering lockcode."
  (let [html (default-get-view-select-lock-code {}) ]
    (is (re-find #"<input[^>]*lockcode.*>" html))))

(def-rpms-test lockcode-tests
  "Testing lockcode submissions"
  (are [doc lockcode path in-session?]
       (testing doc
         (-> {:body-params (if lockcode {:lockcode lockcode} {})}
             default-post-view-select-lock-code
             (redirects? path)
             (is))
         (is (= (if in-session? lockcode nil)
                (session-get :lockcode))))
       "Valid lockcode" "1234" "/view/select/consenter" true
       "Non-numeric" "abba" "/view/select/lock-code" false
       "Bad length" "123" "/view/select/lock-code" false
       "No lock code" nil "/view/select/lock-code" false))
