(ns org.healthsciencessc.consent.collector.process.consent-complete
  (:refer-clojure :exclude [root])
  (:require [org.healthsciencessc.consent.collector.respond :as respond]
            [org.healthsciencessc.consent.collector.text :as text]
            [org.healthsciencessc.consent.collector.process.authorize :as auth]
            [org.healthsciencessc.consent.collector.ui.action :as action]
            [org.healthsciencessc.consent.collector.ui.layout :as layout]
            
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))


;; Register The View Indicating The Consents Are Saved
(defprocess view-consent-complete
  "An overrideable view to indicate the consenting process is complete"
  [ctx]
  (if (auth/is-authenticated?)
    (layout/render-page ctx {:title (text/text :consent.complete.title) :pageid "ConsentComplete"} 
                   [:p (text/text :consent.complete.message)]
                   (action/post-data {:label (text/text :consent.complete.label)
                                      :url "/api/cancel/consent"}))
    (respond/forbidden-view ctx)))

(as-method view-consent-complete endpoint/endpoints "get-view-consent-complete")
