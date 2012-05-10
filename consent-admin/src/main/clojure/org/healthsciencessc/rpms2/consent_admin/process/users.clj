(ns org.healthsciencessc.rpms2.consent-admin.process.users
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.process-engine.path :as path]
            [org.healthsciencessc.rpms2.consent-admin.ui.layout :as layout]
            [org.healthsciencessc.rpms2.consent-admin.config :as config]
            [org.healthsciencessc.rpms2.consent-admin.security :as security]
            [sandbar.stateful-session :as sess]
            [clj-http.client :as http]
            [hiccup.page :as page]
            [hiccup.core :as html]
            [clojure.string :as str]
            [ring.util.response :as rutil])
  (:use [clojure.pprint]
        [org.healthsciencessc.rpms2.consent-admin.services :only (get-users)])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(defn layout-users
  [ctx]
  (let [users (get-users ctx)]
    (with-out-str (pprint users))
    (layout/render ctx "Users"
        [:div.controls
          "edit" "new"]
        [:div#users
          (for [x users]
            [:div.user
              [:h3 (str/join " " [(:first-name x) (:last-name x)])]
              [:ul
                [:li "ID: " (:id x)]
                [:li "Username: " (:username x)]]])])))

(def process-defns
  [{:name "get-view-users"
    :runnable-fn (constantly true)
    :run-fn  layout-users}])

(println "Reloaded users")
(process/register-processes (map #(DefaultProcess/create %) process-defns))
