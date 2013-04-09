(ns org.healthsciencessc.consent.collector.process.witness
  (:refer-clojure :exclude [root])
  (:require [org.healthsciencessc.consent.client.core :as services]
            [org.healthsciencessc.consent.client.whoami :as whoami]
            [org.healthsciencessc.consent.collector.respond :as respond]
            [org.healthsciencessc.consent.collector.state :as state]
            [org.healthsciencessc.consent.collector.text :as text]
            [org.healthsciencessc.consent.collector.ui.layout :as layout]
            [pliant.webpoint.request :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))


;; Register The Witness Signature View
(defprocess view-witness-signatures
  "Creates a view for collecting witness signatures"
  [ctx]
  (if (whoami/identified?)
    (layout/render-page ctx {:title (text/consenter-text :witness.title) :pageid "Witness"
                             :uigenerator "witness-signatures" 
                             :uigenerator-data {:data-submit-url "/api/witness/signatures"
                                                :data-submit-method "POST"
                                                :data-message (text/text :witness.message)
                                                :data-submit-label (text/text :witness.submit.label)}})
    (respond/forbidden-view ctx)))

(as-method view-witness-signatures endpoint/endpoints "get-view-witness-signatures")

;; Register The Witness Signature Service Process
(defprocess api-witness-signatures
  "Persists all of the consent data that has been collected."
  [ctx]
  (if (whoami/identified?)
    (let [body (:body-params ctx)
          resp (services/add-consents (:id (state/get-encounter)) body)]
      (if (meta resp)
        (respond/with-error (text/text :witness.submit.error.message))
        (respond/with-actions {:view-url "/view/consent/complete" :reset false} "changeView")))
    (respond/forbidden-view ctx)))

(as-method api-witness-signatures endpoint/endpoints "post-api-witness-signatures")
