(ns ^{:doc "Witness Signature Collection" }
  org.healthsciencessc.rpms2.consent-collector.witness-consents
  (:require
    [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
    [org.healthsciencessc.rpms2.consent-collector.formutil :as formutil]
    [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only (session-get session-put! flash-get flash-put!)]
        [clojure.tools.logging :only (debug info warn error)]
        [clojure.pprint :only (pprint)]
        [clojure.data.json :only (json-str)]
        [org.healthsciencessc.rpms2.consent-collector.persist :only (persist-session!)]
        [org.healthsciencessc.rpms2.consent-collector.debug :only (debug! pprint-str)]
        [org.healthsciencessc.rpms2.consent-collector.config :only (config)]
        [org.healthsciencessc.rpms2.consent-collector.i18n :only (i18n)]))

(defn protocol-has-witnesses?
  []
  (seq (formutil/witnesses-needed)))

;; check for prescence of code-endorsement-type-witness
;; remove remaining endorsements from session

(defn display-witness-form []
    (helper/rpms2-page 
      (helper/post-form "/witness/consents"
                        (list [:h1 "Witness Consent"]
                              (helper/signaturePadDiv :name "witness"))
                        (helper/submit-btn {:name "witness-submit" :value "Submit"}))
      :title "Witness Consent Form"))

(defn finished-page []
  (persist-session!)
  (helper/rpms2-page
    [:div
     [:h1 (i18n :witness-finished) " Your data has been saved."]
     [:a {:href "/view/select/consenter"} "Return to forever"]]
    :title "Consent Collection Completed"))

(defn view 
  "Returns witness consent form"
  [ctx]
  (if (protocol-has-witnesses?)
    (display-witness-form)
    (finished-page)))
 
(defn perform
  [req]
  (let [sig (-> req :body-params :witness)] ;; this gets the posted PNG signature output
    #_(apply-sig-to-all-endorsements and PERSIST EVERYTHING)
    (session-put! :witness-png sig)
    (finished-page)))

