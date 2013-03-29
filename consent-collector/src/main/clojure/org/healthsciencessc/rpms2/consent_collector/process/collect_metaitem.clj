(ns org.healthsciencessc.rpms2.consent-collector.process.collect-metaitem
  (:refer-clojure :exclude [root])
  (:require [org.healthsciencessc.rpms2.consent-collector.respond :as respond]
            [org.healthsciencessc.rpms2.consent-collector.state :as state]
            [org.healthsciencessc.rpms2.consent-collector.text :as text]
            [org.healthsciencessc.rpms2.consent-collector.process.authorize :as auth]
            [org.healthsciencessc.rpms2.consent-collector.ui.layout :as layout]
            
            [org.healthsciencessc.rpms2.consent-client.core :as services]
            
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))


(defprocess view-collect-metaitem
  "Creates a view for collecting metaitmes"
  [ctx]
  (if (auth/is-authenticated?)
    (layout/render-page ctx {:title (text/consenter-text :collect.metaitem.title) :pageid "CollectMetaItems"
                             :uigenerator "meta-items" :uigenerator-data {:data-submit-url "/api/collect/metaitem"
                                                                          :data-submit-method "POST"
                                                                          :data-submit-label (text/text :action.select.label)}})
    (respond/forbidden-view ctx)))

(as-method view-collect-metaitem endpoint/endpoints "get-view-collect-metaitem")

;; Register The Location Selection Service Process
(defprocess api-collect-metaitem
  "Evaluates the values collected for the metaitems. A map of the meta item ids and their values make up the request body."
  [ctx]
  (if (auth/is-authenticated?)
    (let [protocol-ids (state/get-protocols)
          language-id (state/get-protocol-language)
          protocols (services/get-published-protocol-versions-form protocol-ids language-id)]
      (respond/with-actions {:protocols protocols 
                             :view-url "/view/collect/consent" :reset false}
                             "setProtocols" "changeView"))
    (respond/forbidden-view ctx)))

(as-method api-collect-metaitem endpoint/endpoints "post-api-collect-metaitem")
