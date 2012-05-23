;; Provides the configuration of the protocol managemant UIs.
(ns org.healthsciencessc.rpms2.consent-admin.process.protocol
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
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


(defn view-protocol-location
  "Generates a view that shows all of the protocols available within a location."
  [ctx]
  ;;(if-let [location (get-in ctx [:query-params :location])]
    ;;(let [protocols (services/get-location-protocols location)]
      ;;(if (meta protocols)
      ;;  (rutil/not-found (:message (meta protocols)))
        (layout/render ctx "Protocol List"
          (container/scrollbox (selectlist/selectlist {:label "Protocol A" :data {:id 1 :code "prot1"}}
                                       {:label "Protocol B" :data {:id 2 :code "prot2"}}
                                       {:label "Protocol C" :data {:id 3 :code "prot3"}}))
          (actions/actions 
           (actions/details-button {:url "/view/protocol" :params {:protocol :selected#id}})
           (actions/new-button {:url "/view/protocol/new"})
           (actions/pop-button))))
    ;; Handle Error
    ;;(rutil/not-found "A location is required."))))

(defn view-protocol
  "Generates a view that shows the details of a specific protocol and allows you to edit those details."
  [ctx]
  (layout/render ctx "Protocol"
          (container/scrollbox (form/dataform))
          (actions/actions 
           (actions/details-button {:url "/view/protocol/versions" :params {:protocol :selected#id} :label "Versions"})
           (actions/save-button {:method :post :url "/api/protocol" :params {:protocol :selected#id}})
           (actions/pop-button))))

(defn view-protocol-new
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
   ;; Generates the view for the protocol list in a location.
   {:name "get-view-protocol-location"
    :runnable-fn (constantly true)
    :run-fn view-protocol-location}
   
   ;; Generates the view for a specific protocol.
   {:name "get-view-protocol"
    :runnable-fn (constantly true)
    :run-fn view-protocol}
   
   ;; Generates the view for creating a protocol.
   {:name "get-view-protocol-new"
    :runnable-fn (constantly true)
    :run-fn view-protocol-new}
   ])

(process/register-processes (map #(DefaultProcess/create %) process-defns))
