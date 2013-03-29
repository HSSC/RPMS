(ns org.healthsciencessc.rpms2.consent-collector.process.collect-consent
  (:refer-clojure :exclude [root])
  (:require [org.healthsciencessc.rpms2.consent-collector.common :as common]
            [org.healthsciencessc.rpms2.consent-collector.lock :as locker]
            [org.healthsciencessc.rpms2.consent-collector.respond :as respond]
            [org.healthsciencessc.rpms2.consent-collector.state :as state]
            [org.healthsciencessc.rpms2.consent-collector.text :as text]
            [org.healthsciencessc.rpms2.consent-collector.process.authorize :as auth]
            [org.healthsciencessc.rpms2.consent-collector.ui.layout :as layout]
            [org.healthsciencessc.rpms2.consent-collector.ui.content :as uicontent]
            
            [org.healthsciencessc.rpms2.consent-client.core :as services]
            
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]
            [clojure.string :only [join]]))

(defprocess lock-session
  []
  (locker/lock "/view/locked/request" 
               "/view/collect/consent"
               "/api/collect/consent"
               "/view/unlock/consent" 
               "/api/unlock/consent" 
               "/view/cancel/consent" 
               "/api/cancel/consent"))

;; Register The Collect Consent View
(defprocess view-collect-consent
  "Creates a view for collecting metaitmes"
  [ctx]
  (if (auth/is-authenticated?)
    (do
      (lock-session)
      (layout/render-page ctx {:title (text/consenter-text :collect.consent.title) :pageid "CollectConsent"
                          :header-left "" :header-right ""
                          :uigenerator "collect" :uigenerator-data {:data-submit-url "/api/collect/consent"
                                                                    :data-submit-method "POST"
                                                                    :data-back-label (text/text :collect.consent.back.label)
                                                                    :data-next-label (text/text :collect.consent.next.label)}}))
    (respond/forbidden-view ctx)))

(as-method view-collect-consent endpoint/endpoints "get-view-collect-consent")

;; Register The Collect Consent Service
(defprocess api-collect-consent
  "Processes the values collected by the consent process and forwards on to the review process."
  [ctx]
  (if (auth/is-authenticated?)
    (let [body (:body-params ctx)]
      (respond/with-actions {:view-url "/view/unlock/consent" :reset false} "changeView"))
    (respond/forbidden-view ctx)))

(as-method api-collect-consent endpoint/endpoints "post-api-collect-consent")

