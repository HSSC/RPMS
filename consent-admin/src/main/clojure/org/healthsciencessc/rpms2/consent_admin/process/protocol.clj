;; Provides the configuration of the protocol managemant UIs.
(ns org.healthsciencessc.rpms2.consent-admin.process.protocol
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-admin.security :as security]
            [org.healthsciencessc.rpms2.consent-admin.ui.layout :as layout]
            [org.healthsciencessc.rpms2.consent-domain.roles :as roles]
            [org.healthsciencessc.rpms2.consent-domain.runnable :as runnable]
            [org.healthsciencessc.rpms2.consent-admin.services :as services]
            [org.healthsciencessc.rpms2.consent-admin.ui.container :as container]
            [org.healthsciencessc.rpms2.consent-admin.ui.actions :as actions]
            [org.healthsciencessc.rpms2.consent-admin.ui.selectlist :as selectlist]
            [org.healthsciencessc.rpms2.consent-admin.ui.form :as form]
            [org.healthsciencessc.rpms2.consent-admin.ajax :as ajax]
            [hiccup.core :as hcup]
            [clojure.data.json :as json]
            [ring.util.response :as rutil])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def ^:const fields [{:name :name :label "Protocol Name"}
                     {:name :description :label "Description"}
                     {:name :protocol-id :label "Protocol ID"}
                     {:name :code :label "Code"}
                     {:name :required :label "Required" :type :checkbox}
                     {:name :select-by-default :label "Selected By Default" :type :checkbox}])

(defn view-protocol-location
  "Generates a view that shows all of the protocols available within a location."
  [ctx]
  (if-let [location (get-in ctx [:query-params :location])]
    (let [protocols (services/get-protocols location)]
      (if (meta protocols)
        (rutil/not-found (:message (meta protocols)))
        (layout/render ctx "Protocol List"
          (container/scrollbox (selectlist/selectlist (for [protocol protocols]
                                                        {:label (:name protocol) :data protocol})))
          (actions/actions 
           (actions/details-button {:url "/view/protocol" :params {:location location :protocol :selected#id}})
           (actions/new-button {:url "/view/protocol/new" :params {:location location}})
           (actions/pop-button)))))
    ;; Handle Error
    (layout/render-error ctx {:message "A location is required."})))

(defn view-protocol
  "Generates a view that shows the details of a specific protocol and allows you to edit those details."
  [ctx]
  (if-let [protocol-id (get-in ctx [:query-params :protocol])]
    (let [protocol (services/get-protocol protocol-id)
          location-id (get-in ctx [:query-params :location])]
      (if (not= location-id (get-in protocol [:location :id]))
        (layout/render-error ctx {:message "Location provided must match the location of the protocol requested."})
        (layout/render 
          ctx "Protocol" (container/scrollbox (form/dataform (form/render-fields fields protocol))
          (actions/actions
            (actions/details-button
              {:url "/view/protocol/versions" :params {:protocol protocol-id} :label "Versions"})
            (actions/save-button
              {:method :post :url "/api/protocol" :params {:protocol protocol-id :location location-id}})
            (actions/pop-button))))))
    ;; Handle Error
    (layout/render-error ctx {:message "A location and protocol are required."})))

(defn view-protocol-new
  "Generates a view that allows you to create a new protocol."
  [ctx]
  (let [location (get-in ctx [:query-params :location])]
    (layout/render ctx "Create Protocol"
                   (container/scrollbox (form/dataform (form/render-fields fields)))
                   (actions/actions 
                     (actions/save-button {:method :post :url "/api/protocol" :params {:location location}})
                     (actions/pop-button)))))

(defn api-add-protocol
  "Adds a new protocol to a location."
  [ctx]
  (if-let [location-id (get-in ctx [:query-params :location])]
    (let [body (select-keys (:body-params ctx) (map :name fields))
          location-role (first (roles/protocol-designer-mappings :location {:id location-id}))
          location (:location location-role)
          protocol (assoc body :organization (:organization location) :location location)
          resp (services/add-protocol protocol)]
      ;; Handle Error or Success
      (if (services/service-error? resp)
        (ajax/save-failed (meta resp))
        (ajax/success resp)))
    ;; Handle Bad Request
    (ajax/error {:message "A location is required."})))

(defn api-update-protocol
  "Adds a new protocol to a location."
  [ctx]
  (if-let [protocol-id (get-in ctx [:query-params :protocol])]
    (let [body (select-keys (:body-params ctx) (map :name fields))
          resp (services/update-protocol protocol-id body)]
      ;; Handle Error or Success
      (if (services/service-error? resp)
        (ajax/save-failed (meta resp))
        (ajax/success resp)))
    ;; Handle Bad Request
    (ajax/error {:message "A location is required."})))

(def process-defns
  [
   ;; Generates the view for the protocol list in a location.
   {:name "get-view-protocol-location"
    :runnable-fn (runnable/gen-designer-location-check security/current-user)
    :run-fn view-protocol-location
    :run-if-false ajax/forbidden}
   
   ;; Generates the view for a specific protocol.
   {:name "get-view-protocol"
    :runnable-fn (runnable/gen-designer-location-check security/current-user)
    :run-fn view-protocol
    :run-if-false ajax/forbidden}
   
   ;; Generates the view for creating a protocol.
   {:name "get-view-protocol-new"
    :runnable-fn (runnable/gen-designer-location-check security/current-user)
    :run-fn view-protocol-new
    :run-if-false ajax/forbidden}
   
   ;; Generates the api service for creating a protocol.
   {:name "put-api-protocol"
    :runnable-fn (runnable/gen-designer-location-check security/current-user)
    :run-fn api-add-protocol
    :run-if-false ajax/forbidden}
   
   ;; Generates the api service for creating a protocol.
   {:name "post-api-protocol"
    :runnable-fn (runnable/gen-designer-location-check security/current-user)
    :run-fn api-update-protocol
    :run-if-false ajax/forbidden}
   ])

(process/register-processes (map #(DefaultProcess/create %) process-defns))
