(ns org.healthsciencessc.rpms2.consent-collector.process.select-protocol
  (:refer-clojure :exclude [root])
  (:require [org.healthsciencessc.rpms2.consent-collector.respond :as respond]
            [org.healthsciencessc.rpms2.consent-collector.state :as state]
            [org.healthsciencessc.rpms2.consent-collector.text :as text]
            [org.healthsciencessc.rpms2.consent-collector.process.authorize :as auth]
            [org.healthsciencessc.rpms2.consent-collector.ui.action :as action]
            [org.healthsciencessc.rpms2.consent-collector.ui.form :as form]
            [org.healthsciencessc.rpms2.consent-collector.ui.layout :as layout]
            
            [org.healthsciencessc.rpms2.consent-client.core :as services]
            
            [org.healthsciencessc.rpms2.consent-domain.roles :as roles]
            [org.healthsciencessc.rpms2.consent-domain.tenancy :as tenancy]
            
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint]
            
            [clojure.string :as string])
  (:use     [pliant.process :only [defprocess as-method]]))


(def fields [{:name :protocols :type :checklist :label "Protocols" :contain true
              :items [{:label "Value 1" :value "1"}
                      {:label "Value 2" :value "2"}
                      {:label "Value 3" :value "3"}]}
             {:name :language :type :radio :label (text/text :select.protocol.language.label) :contain true}])

(def form-options {:method :post
                   :url "/api/select/protocol"})

(defn protocol-sorter
  [a b] 
  (cond
    (not= (:disabled a) (:disabled b)) (if (:disabled a) true false)
    (not= (:checked a) (:checked b)) (if (:checked a) true false)
    :else (compare (:label a) (:label b))))


;; Register The Select Protocol View
(defprocess view-select-protocol
  "Creates a view of protocols to be selected."
  [ctx]
  (if (auth/is-authenticated?)
    (let [location (state/get-location)
          location-id (:id location)
          protocol-versions (services/get-published-protocol-versions location-id)]
      (if (> (count protocol-versions) 0)
        (let [protocol-items (for [prot protocol-versions] {:value (:id prot) :label (get-in prot [:protocol :name])
                                                            :disabled (get-in prot [:protocol :required])
                                                            :checked (or (get-in prot [:protocol :required])
                                                                         (get-in prot [:protocol :select-by-default]))})
              languages (distinct (flatten (map :languages protocol-versions)))
              language-items (for [lang languages] {:value (:id lang) :label (:name lang)})
              data {:language (get-in location [:organization :language :id])}]
          (layout/render-page ctx {:title (text/protocol-text :select.protocol.title) :pageid "SelectProtocols"} 
                         (form/dataform form-options 
                                        (form/render-fields {:fields {:protocols {:items (sort protocol-sorter protocol-items)}
                                                                      :language {:items language-items}}} fields data)
                                        (action/form-submit {:label (text/consenter-text :select.protocol.ok.label)}))))
        (respond/redirect ctx "/view/select/protocol/none")))
    (respond/forbidden-view ctx)))

(as-method view-select-protocol endpoint/endpoints "get-view-select-protocol")

;; Register View To Handle When A Location Has No Published Protocols.
(defprocess view-select-protocol-none
  "Creates a view to inform that there are no protocols to select."
  [ctx]
  (layout/render-page ctx {:title (text/protocol-text :select.protocol.none.title) :pageid "SelectProtocolsNone"} 
                 [:h3 (text/protocol-text :select.protocol.none.message)]))

(as-method view-select-protocol-none endpoint/endpoints "get-view-select-protocol-none")

;; Register API Service To Select Protocols
(defprocess api-select-protocol
  "Processes the protocols selected."
  [ctx]
  (if (auth/is-authenticated?)
    (let [protocol-ids (get-in ctx [:body-params :protocols])
          language-id (get-in ctx [:body-params :language])]
      (cond
        (nil? (seq protocol-ids)) (respond/with-error (text/location-text :select.protocol.message.notvalid))
        (nil? (seq language-id)) (respond/with-error (text/location-text :select.protocol.message.notvalid))
        :else
        (let [language (services/get-language language-id)
              meta-items (services/get-published-protocol-versions-meta protocol-ids)]
          (state/set-protocols protocol-ids language-id)
          (respond/with-actions {:language language :protocol-ids protocol-ids :meta-items meta-items
                                 :view-url "/view/collect/metaitem" :reset false}
                                "setProtocolIds" "setLanguage" "setMetaItems" "changeView"))))
    (respond/forbidden-view ctx)))

(as-method api-select-protocol endpoint/endpoints "post-api-select-protocol")
