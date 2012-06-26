(ns ^{:doc "Collect consents - collects information from forms." }
  org.healthsciencessc.rpms2.consent-collector.witness-consents
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.mock :as mock]
   [org.healthsciencessc.rpms2.consent-collector.formutil :as formutil]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info warn error)])
  (:use [clojure.pprint :only (pprint)])
  (:use [clojure.data.json :only (json-str)])
  (:use [org.healthsciencessc.rpms2.consent-collector.debug :only [debug! pprint-str]])
  (:use [org.healthsciencessc.rpms2.consent-collector.config :only [config]])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

(defn collector-name []
  (let [{:keys [first-name last-name]} (session-get :user)]
    (str first-name " " last-name)))


(defn view 
  "Returns witness consent form"
  [ctx]

  (helper/rpms2-page 
     (helper/post-form "/view/unimplemented" 
        (list [:h1 "Witness Consent" ]
              (helper/signaturePadDiv "FIXTHISNAME" nil))

        (helper/submit-btn { :value "Submit" } ))
   :title "Witness Consent Form" ))

(defn perform
  "Collect consents."

  [{parms :body-params :as ctx}]

  (helper/myredirect "/view/unimplemented"))
