(ns org.healthsciencessc.rpms2.consent-admin.process.locations
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.process-engine.path :as path]
            [org.healthsciencessc.rpms2.consent-admin.ui.layout :as layout]
            [org.healthsciencessc.rpms2.consent-admin.config :as config]
            [org.healthsciencessc.rpms2.consent-admin.ajax :as ajax]
            [org.healthsciencessc.rpms2.consent-admin.security :as security]
            [org.healthsciencessc.rpms2.consent-admin.ui.container :as container]
            [org.healthsciencessc.rpms2.consent-admin.ui.actions :as actions]
            [org.healthsciencessc.rpms2.consent-admin.ui.selectlist :as selectlist]
            [org.healthsciencessc.rpms2.consent-admin.ui.form :as formui]
            [org.healthsciencessc.rpms2.consent-domain.tenancy :as tenancy]
            [sandbar.stateful-session :as sess]
            [org.healthsciencessc.rpms2.consent-admin.services :as service]
            [hiccup.core :as html]
            [hiccup.element :as elem]
            [hiccup.form :as form]
            [ring.util.response :as rutil])
  (:use [clojure.pprint])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(defn- render-label
  [& addons]
  (let [org (security/current-org)
        label (tenancy/label-for-location nil org)]
    (str label (apply str addons))))

(defn layout-locations
  [ctx]
  (let [locations (sort-by :name (service/get-locations))]
    (if (service/service-error? locations)
      (ajax/error (meta locations))
      (layout/render ctx (render-label "s")
        (container/scrollbox
          (selectlist/selectlist {:action :.detail-action}
            (for [x locations]
              {:label (:name x) :data x})))
        (actions/actions
             (actions/details-action {:url "/view/location/edit" :params {:location :selected#id}})
             (actions/new-action {:url "/view/location/add"})
             (actions/back-action))))))

(def location-fields
  (let [text-fields [:name "Name"
                     :code "Code"
                     :protocol-label "Protocol Label"
                     :consenter-label "Consenter Label"]]
    (map #(zipmap [:name :label] %)
         (partition 2 text-fields))))

(defn default-protocol-label []
  (get-in (sess/session-get :user)
    [:organization :protocol-label]))

(defn render-location-fields
  "Create some field boxes from a map of [kw text-label]"
  ([] (render-location-fields {:protocol-label (default-protocol-label)}))
  ([location]
    (map formui/record->editable-field
         (repeat location)
         location-fields)))

(defn get-view-location-add
  [ctx]
  (layout/render ctx (str "Create " (render-label))
                 (container/scrollbox (formui/dataform (render-location-fields)))
                 (actions/actions
                   (actions/save-action {:method :post :url "/api/location/add"})
                   (actions/back-action))))

(defn get-view-location-edit
  [ctx]
  (if-let [location-id (-> ctx :query-params :location)]
    (let [location (service/get-location location-id)]
      (if (service/service-error? location)
        (ajax/error (meta location))
        (layout/render ctx (str "Edit " (render-label))
                   (container/scrollbox (formui/dataform (render-location-fields location)))
                   (actions/actions
                     (actions/delete-action {:url "/api/location" :params {:location location-id}})
                     (actions/save-action {:method :post :url "/api/location/edit" :params {:location location-id}})
                     (actions/back-action)))))))


(defn delete-api-location
  [ctx]
  (let [loc-id (:location (:query-params ctx))
        resp (service/delete-location loc-id)]
    (if (service/service-error? resp)
      (ajax/error (meta resp))
      (ajax/success resp))))


(defn post-api-location-add
  [ctx]
  (let [location (select-keys (:body-params ctx)
                          (map :name location-fields))
        resp (service/add-location location)]
    (if (service/service-error? resp)
      (ajax/error (meta resp))
      (ajax/success resp))))

(defn post-api-location-edit
  [ctx]
  (let [keys (select-keys (:body-params ctx)
                          (map :name location-fields))
        resp (service/edit-location (-> ctx :query-params :location)
                                keys)]
    (if (service/service-error? resp)
      (ajax/error (meta resp))
      (ajax/success resp))))

(def process-defns
  [{:name "get-view-locations"
    :runnable-fn (constantly true)
    :run-fn  layout-locations}
   {:name "get-view-location-add"
    :runnable-fn (constantly true)
    :run-fn get-view-location-add}
   {:name "get-view-location-edit"
    :runnable-fn (constantly true)
    :run-fn get-view-location-edit}
   {:name "delete-api-location"
    :runnable-fn (constantly true)
    :run-fn delete-api-location}
   {:name "post-api-location-edit"
    :runnable-fn (constantly true)
    :run-fn post-api-location-edit}
   {:name "post-api-location-add"
    :runnable-fn (constantly true)
    :run-fn post-api-location-add}
   ])

(process/register-processes (map #(DefaultProcess/create %) process-defns))
