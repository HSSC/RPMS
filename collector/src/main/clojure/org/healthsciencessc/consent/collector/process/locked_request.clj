(ns org.healthsciencessc.consent.collector.process.locked-request
  (:refer-clojure :exclude [root])
  (:require [org.healthsciencessc.consent.client.whoami :as whoami]
            [org.healthsciencessc.consent.collector.respond :as respond]
            [org.healthsciencessc.consent.collector.text :as text]
            [org.healthsciencessc.consent.collector.ui.action :as action]
            [org.healthsciencessc.consent.collector.ui.layout :as layout]
            [pliant.webpoint.request :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))


;; Register The Consent Unauthorized View
(defprocess view-locked-request
  "Creates a view of to set the lockcode"
  [ctx]
  (if (whoami/identified?)
    (layout/render-page ctx {:title (text/text :locked.unauthorized.title) :pageid "LockedRequest"} 
                   [:p (text/text :locked.unauthorized.message)]
                   (action/wrapper
                     (action/post-data {:label (text/text :locked.unauthorized.ok.label)
                                            :url "/api/cancel/consent"})))
    (respond/forbidden-view ctx)))

(as-method view-locked-request endpoint/endpoints "get-view-locked-request")
