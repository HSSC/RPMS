(ns org.healthsciencessc.rpms2.consent-collector.test.core
  (:import (com.gargoylesoftware.htmlunit WebClient BrowserVersion))
  (:require [org.healthsciencessc.rpms2.process-engine.core :as pe]
            [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
            [org.healthsciencessc.rpms2.consent-collector.test.factories :as factories]
            [net.cgrand.enlive-html :as en])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]])
  (:use [sandbar.stateful-session :only (session-get)])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [clojure.data.json :only (read-json json-str)]
        org.healthsciencessc.rpms2.consent-collector.test.helpers)
  (:require [org.healthsciencessc.rpms2.consent-collector
             [login :as login]
             [select-location :as select-location]
             [select-consenter :as select-consenter]
             [search-consenter :as search-consenter]
             [select-protocol :as select-protocol]
             [metadata :as metadata]
             [create-consenter :as create-consenter]
             [core :as core]
             [select-lockcode :as select-lockcode]])
  (:use clojure.test))

(use-fixtures :each setup-session-and-flash)

(defn redirects?
  "Returns true if the response map is a redirect to the
  given location."
  [resp location]
  (and
   (map? resp)
   (= (resp :status) 302)
   (= (get-in resp [:headers "Location"]) location)))

(deftest get-login-test
  (testing "Test that login maps to view/login, with a 302 status."
    (is (redirects? (login/default-get-login {}) "/view/login"))))

(deftest get-view-login-test
  "Test that an HTML login page is returned."
  (let [html (login/view {})]
    (is (page-has? html [[:input (en/attr= :name "password")]]))))

(deftest post-view-login-test
  (testing "Test authentication."
    (are [doc status location]
         (testing doc
           (with-redefs [dsa/authenticate
                         (constantly {:status status})]
             (let [resp (login/perform {:userid "foobar", :password "hunter2"})]
               (is (redirects? resp location)))))
         "Authentication succeeds" 200 "/view/select/location"
         "Authentication fails" 401 "/view/login"
         "User doesn't exist" 404 "/view/login")))


(deftest determine-users-location-test
  (are [doc locations path text]
       (testing doc
         (with-session {:user (apply factories/user-with-locations locations)}
           (-> (select-location/view {})
               ((fn [resp]
                  (if path
                    (redirects? resp path)
                    (page-has-text? resp text))))
               (is))))
       "No authorized locations" [] "/view/not-authorized" nil
       "One location" ["Hardy har"] "/view/select/lock-code" nil
       "Many locations" ["foo" "bar" "baz"] nil "foo"))

(deftest get-view-not-authorized-test
  (let [msg (i18n :not-authorized-message)]
    (is msg)
    (->> (login/default-get-view-not-authorized {})
         (re-find (re-pattern msg))
         (is))))

(deftest authorized-locations-test
  (let [user (factories/user-with-roles-and-locations
               ["Consent Collector" "foo"
                "Party Thrower"     "bar"
                "Consent Collector" "baz"])
        locs (select-location/authorized-locations user)]
    (= #{"foo" "baz"} (->> locs (map :name) set))))

;;======================================
;; Test Select Lock Code 
;; Cases: 
;; 1. the html form
;; 2. the submitted html form, with valid 4 digit lockcode
;; 3. the submitted html form, with invalid lockcode
;; 4. the submitted html form, with no lockcode
;; will go to /view/select/consenter
(deftest get-view-select-lockcode
  (testing "Test that select-lockcode displays a valid input form for entering lockcode."
    (let [html (select-lockcode/view {}) ]
      (is (re-find #"<input[^>]*lockcode.*>" html)))))

(deftest lockcode-tests
  (testing "Testing lockcode submissions"
    (are [doc lockcode path in-session?]
         (testing doc
           (-> {:body-params (if lockcode {:lockcode lockcode} {})}
               select-lockcode/perform
               (redirects? path)
               (is))
           (is (= (if in-session? lockcode nil)
                  (session-get :lockcode))))
         "Valid lockcode" "1234" "/view/select/consenter" true
         "Non-numeric" "abba" "/view/select/lock-code" false
         "Bad length" "123" "/view/select/lock-code" false
         "No lock code" nil "/view/select/lock-code" false)))

#_(deftest view-select-consenter-test
  (let [html (select-consenter/view {})]
    (are [sel] (page-has? html sel)
         [[:form (en/attr= :action "/view/search/consenters")]]
         [[:form (en/attr= :action "/view/create/consenter")]])))

#_(deftest view-search-consenters-test
  (with-redefs [search-consenter/search-consenters
                (constantly {:status 200
                             :json [{:first-name "FOO" :last-name "BAR"}
                                    {:first-name "BAZ" :last-name "BAM"}]})]
    (let [html (search-consenter/get-view {})]
      (are [text] (page-has-text? html text)
           "FOO BAR"
           "BAZ BAM"))))

(deftest view-create-consenter-test
  (let [html (create-consenter/view {})
        [form] (en/select (en/html-snippet html)
                          [[:form (en/attr= :action "/view/create/consenter")]])]
    (is form)
    (are [sel] (is (not (empty? (en/select form sel))))
         [[:form (en/attr= :action "/view/create/consenter")]]
         [[:input (en/attr= :name "first-name")]]
         [[:input (en/attr= :name "last-name")]])))
