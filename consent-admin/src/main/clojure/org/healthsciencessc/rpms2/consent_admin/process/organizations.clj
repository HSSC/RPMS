(ns org.healthsciencessc.rpms2.consent-admin.process.organizations
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.process-engine.path :as path]
            [org.healthsciencessc.rpms2.consent-admin.ui.layout :as layout]
            [org.healthsciencessc.rpms2.consent-admin.config :as config]
            [org.healthsciencessc.rpms2.consent-admin.security :as security]
            [sandbar.stateful-session :as sess]
            [org.healthsciencessc.rpms2.consent-admin.services :as service]
            [hiccup.core :as html]
            [ring.util.response :as rutil])
  (:use [clojure.pprint]
        [org.healthsciencessc.rpms2.consent-admin.services :only (get-organizations)])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(defn layout-organizations
  [params]
  (let [orgs (get-organizations params)]
    (html/html
      (layout/pane nil "Organizations"
        (for [x orgs]
          [:div.organization
            [:pre (with-out-str (pprint x))]
            [:h3 (:name x)]
            [:ul
              [:li "ID: " (:id x)]
              [:li "Location-label" (:id x)]
              [:li "Code: " (:code x)]]])))))

(def process-defns
  [{:name "get-view-organizations"
    :runnable-fn (constantly true)
    :run-fn  layout-organizations}
   {:name "get-processes"
    :runnable-fn (constantly true)
    :run-fn (fn [params]
              (with-out-str (pprint @process/default-processes)))}])

(println "Reloaded orgs")

(process/register-processes (map #(DefaultProcess/create %) process-defns))
