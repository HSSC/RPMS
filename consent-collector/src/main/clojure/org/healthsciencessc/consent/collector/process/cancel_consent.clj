(ns org.healthsciencessc.consent.collector.process.cancel-consent
  (:refer-clojure :exclude [root])
  (:require [org.healthsciencessc.consent.collector.respond :as respond]
            [org.healthsciencessc.consent.collector.lock :as lock]
            [org.healthsciencessc.consent.collector.state :as state]
            [org.healthsciencessc.consent.collector.text :as text]
            [org.healthsciencessc.consent.collector.process.authorize :as auth]
            [org.healthsciencessc.consent.collector.ui.action :as action]
            [org.healthsciencessc.consent.collector.ui.content :as cont]
            [org.healthsciencessc.consent.collector.ui.layout :as layout]
            
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))

;; Register The Consent Cancel View
(defprocess view-consent-cancel
  "Creates a view of to set the lockcode"
  [ctx]
  (if (auth/is-authenticated?)
    (layout/render-dialog ctx {:title (text/text :cancel.consent.title) :dialogid "ConsentCancel"} 
                   (cont/paragraph (text/text :cancel.consent.message))
                   (action/post-data {:label (text/text :cancel.consent.yes.label)
                                            :url "/api/cancel/consent"})
                   (action/back {:label (text/text :cancel.consent.no.label)}))
    (respond/forbidden-view ctx)))

(as-method view-consent-cancel endpoint/endpoints "get-view-cancel-consent")


(defprocess api-consent-cancel
  "Provides the process for cancelling a session before it has been completed."
  [ctx]
  (if (auth/is-authenticated?)
    (do
      (lock/unlock)
      (state/reset-consent-session)
      (respond/with-actions {:view-url "/view/search/consenter" :reset false} "resetSession" "changeView"))
    (respond/forbidden-view ctx)))

(as-method api-consent-cancel endpoint/endpoints "post-api-cancel-consent")