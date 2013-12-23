(ns org.healthsciencessc.consent.collector.process.collect-consent
  (:refer-clojure :exclude [root])
  (:require [org.healthsciencessc.consent.client.core :as services]
            [org.healthsciencessc.consent.client.whoami :as whoami]
            [org.healthsciencessc.consent.collector.common :as common]
            [org.healthsciencessc.consent.collector.lock :as locker]
            [org.healthsciencessc.consent.collector.respond :as respond]
            [org.healthsciencessc.consent.collector.state :as state]
            [org.healthsciencessc.consent.collector.text :as text]
            [org.healthsciencessc.consent.collector.ui.layout :as layout]
            [org.healthsciencessc.consent.collector.ui.content :as uicontent]
            [pliant.webpoint.request :as endpoint])
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
  (if (whoami/identified?)
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
  (if (whoami/identified?)
    (let [body (:body-params ctx)]
      (respond/with-actions {:view-url "/view/unlock/consent" :reset false} "changeView"))
    (respond/forbidden-api ctx)))

(as-method api-collect-consent endpoint/endpoints "post-api-collect-consent")

