(ns org.healthsciencessc.rpms2.consent-collector.test.scenario
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [clojure.data.json :only (read-json json-str)]
        org.healthsciencessc.rpms2.consent-collector.test.helpers
        org.healthsciencessc.rpms2.consent-collector.test.scenario-helpers)
  (:use clojure.test))

(def-rpms-test use-case-consent-collector-logs-into-consent-system-login-one-site
  "Consent Collector opens the browser to the RPMS Consent site.
   Consent Collector is redirected to SSO login page and logs in with credentials.
   [Tenancy] Consent Collector selects a ‘Location’ they had authority for which they will be at for the session.
   [Security] Consent Collector enters the ‘Lock Code’ that is used to lock the Consentor out of the application when the Consentor is filling out permissions.
   Consent Collector is redirected to Consentor Lookup landing page.

   Cases: 
	1) user information is not valid (username, password)
	2) user has no authorized locations 
	2) user has one authorized locations 
	3) user has multiple authorized locations 
	4) user enters valid lockcode
	4) user enters invalid lockcode
   "
    
  (with-client page (url "/view/login")
    (-> page
        (fill-out-and-submit-first-form {"userid" "foo"
                                         "password" "bar"})
        (should-be-on-page "/view/select/location")
        (fill-out-and-submit-first-form {"location" "Registration"})
        (should-be-on-page "/view/select/lock-code")
        (fill-out-and-submit-first-form {"lockcode" "9876"})
        (should-be-on-page "/view/select/consenter"))))
