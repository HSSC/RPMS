;; Provides the configuration of the protocol managemant UIs.
(ns org.healthsciencessc.rpms2.consent-admin.process.protocol-version
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-admin.security :as security]
            [org.healthsciencessc.rpms2.consent-admin.ui.layout :as layout]
            [org.healthsciencessc.rpms2.consent-domain.runnable :as runnable]
            [org.healthsciencessc.rpms2.consent-admin.services :as services]
            [org.healthsciencessc.rpms2.consent-admin.ui.container :as container]
            [org.healthsciencessc.rpms2.consent-admin.ui.actions :as actions]
            [org.healthsciencessc.rpms2.consent-admin.ui.selectlist :as selectlist]
            [org.healthsciencessc.rpms2.consent-admin.ui.form :as form]
            [hiccup.core :as hcup]
            [clojure.data.json :as json]
            [ring.util.response :as rutil])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))


(defn view-protocol-versions
  "Generates a view that shows a list of the available versions of a protocol."
  [ctx]
  (let [protocol-id (get-in ctx [:query-params :protocol])
        ;;versions (service/get-protocol-versions protocol-id)
        ]
  (layout/render ctx "Protocol Version"
                 (container/scrollbox (selectlist/selectlist 
                                   
                                        {:label "Version A [retired]" :data {:id 1 :code "prot1"}}
                                       {:label "Version B [published]" :data {:id 2 :code "prot2"}}
                                       {:label "Version C [draft]" :data {:id 3 :code "prot3"}}))
                 (actions/actions 
                   (actions/details-button {:url "/view/protocol/version" :params {:version :selected#id}})
                   (actions/new-button {:url "/view/protocol/version/new"})
                   (actions/pop-button)))))

(defn view-protocol-version
  "Generates a view that shows a list of the available versions of a protocol."
  [ctx]
  (let [id (get-in ctx [:query-params :version])
        ;;version (service/get-protocol-version id)
        ;;editable (= (:status version) "draft")
        ]
    (layout/render ctx "Protocol Version"
                   (container/scrollbox (selectlist/selectlist {:label "Version A [retired]" :data {:id 1 :code "prot1"}}
                                         {:label "Version B [published]" :data {:id 2 :code "prot2"}}
                                         {:label "Version C [draft]" :data {:id 3 :code "prot3"}}))
                   (actions/actions 
                     (actions/details-button {:url "/view/protocol/version" :params {:protocol :selected#id}})
                     ;;(if editable (actions/save-button {:method :post :url "/api/protocol/version" :params {:version id}}))
                     (actions/pop-button)))))

(defn view-protocol-version-new
  "Generates a view that allows you to create a new protocol."
  [ctx]
  (layout/render ctx "Create Protocol"
                 (container/scrollbox (form/dataform (list 
                                 (form/input-hidden {:name :id :value "12345679101112"})
                                 (form/input-text {:name :name :label "Name"})
                                 (form/input-text {:name :code :label "Code"})
                                 (form/input-text {:name :protocol-id :label "Protocol ID"})
                                 (form/input-text {:name :description :label "Description"})
                                 (form/input-checkbox {:name :required :label "Required" :checked false})
                                 (form/input-checkbox {:name :select-by-default :label "Select By Default" :checked false}))))
                 (actions/actions 
                   (actions/save-button {:method :post :url "/api/protocol" :params {:protocol :selected#id}})
                   (actions/pop-button))))

(def process-defns
  [
   ;; Generates the view for a specific protocol version.
   {:name "get-view-protocol-versions"
    :runnable-fn (runnable/gen-designer-location-check security/current-user)
    :run-fn view-protocol-versions}
   
   ;; Generates the view for creating a protocol.
   {:name "get-view-protocol-version-new"
    :runnable-fn (runnable/gen-designer-location-check security/current-user)
    :run-fn view-protocol-version-new}
   ])

(process/register-processes (map #(DefaultProcess/create %) process-defns))
