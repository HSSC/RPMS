(ns org.healthsciencessc.rpms2.consent-admin.process.locations
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.process-engine.path :as path]
            [org.healthsciencessc.rpms2.consent-admin.ui.layout :as layout]
            [org.healthsciencessc.rpms2.consent-admin.security :as security]
            [hiccup.core :as html]
            [ring.util.response :as rutil])
  (:use [clojure.pprint]
        [org.healthsciencessc.rpms2.consent-admin.services :only (get-locations)])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(defn layout-locations
  [params]
  (let [locs (get-locations params)]
    (html/html
      (layout/pane nil "Locations" 
        [:div (with-out-str (pprint locs))
          (for [x locs]
            [:div.location
              [:h3 (:name x)]
              [:ul
                [:li "ID: " (:id x)]
                [:li "Protocol Label" (:protocol-label x)]
                [:li "Code: " (:code x)]]])]))))

(def process-defns
  [{:name "get-view-locations"
    :runnable-fn (constantly true)
    :run-fn layout-locations}])

(println "Reloaded locs")

(process/register-processes (map #(DefaultProcess/create %) process-defns))
