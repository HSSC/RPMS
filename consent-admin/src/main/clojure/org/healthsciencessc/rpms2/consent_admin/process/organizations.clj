(ns org.healthsciencessc.rpms2.consent-admin.process.organizations
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.process-engine.path :as path]
            [org.healthsciencessc.rpms2.consent-admin.ui.layout :as layout]
            [org.healthsciencessc.rpms2.consent-admin.config :as config]
            [org.healthsciencessc.rpms2.consent-admin.security :as security]
            [sandbar.stateful-session :as sess]
            [noir.validation :as val]
            [org.healthsciencessc.rpms2.consent-admin.services :as service]
            [hiccup.core :as html]
            [hiccup.element :as elem]
            [hiccup.form :as form]
            [ring.util.response :as rutil])
  (:use [clojure.pprint]
;        [org.healthsciencessc.rpms2.consent-admin.mf :only (mf)]
        [org.healthsciencessc.rpms2.consent-admin.services :only (get-organizations)])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(defn layout-organizations
  [ctx]
  (let [orgs (get-organizations ctx)]
    (layout/render ctx "Organizations"
      [:div#org-func.controls
       [:div#new-org "+"]
       [:div#edit-org "Edit"]
       [:div#add-admin "Add Administrator"]
       [:div.done-button.ui-state-error "Done"]]
      [:div.scroll-container
      [:div#org-list
        (for [x orgs]
          [:div.organization {:data-org-id (:id x)}
  ;         mf
            [:h3 (:name x)]
            [:ul
              [:li "Protocol-label: " (:protocol-label x)]
              [:li "Location-label" (:location-label x)]
              [:li "Code: " (:code x)]]])]]
              [:script {:src "/js/org.js"}])))

(defn with-error [key & content]
  (let [err (seq (val/get-errors key))]
    (if err
      (list [:div.form-error err]
        content)
      content)))

(defn create-fields [{:keys [name code protocol-label location-label]}]
  (list
    (with-error :name
      (form/label "name" "Name")
      (form/text-field "name" name))
    (with-error :code
      (form/label "code" "Code")
      (form/text-field "code" code))
    (with-error :location-label
      (form/label "protocol-label" "Protocol Label")
      (form/text-field "protocol-label" protocol-label))
    (with-error :location-label
      (form/label "location-label" "Location Label")
      (form/text-field "location-label" location-label))))

(defn get-view-organization-add
  [ctx]
  (layout/render ctx "Create Organization"
    [:div#add-form-status.ui-state-error]
    [:form#add-org-form
      (create-fields {})
      [:div.save-button "Save"]
      [:div.cancel-button "Save"]
      [:script {:src "/js/org-add.js"}]]))

(defn validate-add-org
  [{:keys [name code protocol-label location-label]}]
  (val/rule (< 2 (count name)) [:name "Organization name needs to be greater than two characters"])
  (val/rule (< 0 (count code)) [:code "Code needs to be present"]))

(defn get-view-organization-edit
  [ctx]
  (if-let [org-id (-> ctx :query-params :organization)]
    (let [org (service/get-organization org-id)]
      (layout/render ctx "Edit Organization"
        ;[:pre (with-out-str (pprint ctx) (pprint org))]
        [:div#edit-form-status.ui-state-error]
        (form/form-to [:post (str "/api/organization/edit?organization=" org-id)]
           (create-fields org)
           [:div#save-button "Save"])
           [:script {:src "/js/org-edit.js"}]))))

(defn ajax-status
  [{valid :valid}]
  (if valid
    "Saved!"
    "Something went wrong..."))

(defn post-api-organization-add
  [ctx]
;;  (validate-add-org (:body-params ctx))
;;  (if (val/errors?)
;;    (process/dispatch "get-add-organization" ctx)
;;    (do
  (let [org (select-keys (:body-params ctx)
                         [:name :code :location-label])
        resp (service/add-organization org)]
    (ajax-status resp)))

(defn post-api-organization-edit
  [ctx]
;;  (validate-add-org (:body-params ctx))
;;  (if (val/errors?)
;;    (process/dispatch "get-edit-organization" ctx)
;;    (do
  (let [keys (select-keys (:body-params ctx)
                              [:name :code :protocol-label :location-label])
        resp (service/edit-organization (-> ctx :query-params :organization) keys)]
    (ajax-status resp)))
        ;;(process/dispatch "get-view-organizations" ctx))))

(def process-defns
  [{:name "get-view-organizations"
    :runnable-fn (constantly true)
    :run-fn  layout-organizations}
   {:name "get-view-organization-add"
    :runnable-fn (constantly true)
    :run-fn get-view-organization-add}
   {:name "get-view-organization-edit"
    :runnable-fn (constantly true)
    :run-fn get-view-organization-edit}
   {:name "post-api-organization-edit"
    :runnable-fn (constantly true)
    :run-fn post-api-organization-edit}
   {:name "post-api-organization-add"
    :runnable-fn (constantly true)
    :run-fn post-api-organization-add}
   ])

(println "Reloaded orgs")

(process/register-processes (map #(DefaultProcess/create %) process-defns))
