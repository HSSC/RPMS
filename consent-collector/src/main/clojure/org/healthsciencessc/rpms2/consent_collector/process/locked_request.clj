(ns org.healthsciencessc.rpms2.consent-collector.process.locked-request
  (:refer-clojure :exclude [root])
  (:require [org.healthsciencessc.rpms2.consent-collector.respond :as respond]
            [org.healthsciencessc.rpms2.consent-collector.text :as text]
            [org.healthsciencessc.rpms2.consent-collector.process.authorize :as auth]
            [org.healthsciencessc.rpms2.consent-collector.ui.action :as action]
            [org.healthsciencessc.rpms2.consent-collector.ui.layout :as layout]
            
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))


;; Register The Consent Unauthorized View
(defprocess view-locked-request
  "Creates a view of to set the lockcode"
  [ctx]
  (if (auth/is-authenticated?)
    (layout/render-page ctx {:title (text/text :locked.unauthorized.title) :pageid "LockedRequest"} 
                   [:p (text/text :locked.unauthorized.message)]
                   (action/post-data {:label (text/text :locked.unauthorized.ok.label)
                                            :url "/api/cancel/consent"}))
    (respond/forbidden-view ctx)))

(as-method view-locked-request endpoint/endpoints "get-view-locked-request")
