(ns org.healthsciencessc.rpms2.consent-collector.process.dev-endpoints
  (:refer-clojure :exclude [root])
  (:require [org.healthsciencessc.rpms2.consent-collector.respond :as respond]
            [org.healthsciencessc.rpms2.consent-collector.state :as state]
            [org.healthsciencessc.rpms2.consent-collector.text :as text]
            [org.healthsciencessc.rpms2.consent-collector.process.authorize :as auth]
            [org.healthsciencessc.rpms2.consent-collector.ui.action :as action]
            [org.healthsciencessc.rpms2.consent-collector.ui.form :as form]
            [org.healthsciencessc.rpms2.consent-collector.ui.layout :as layout]
            
            [hiccup.page :as page]
            
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))


;; Register The Select Lockcode View
(defprocess view-session
  "Creates a view of what is in the server session."
  [ctx]
  (if (auth/is-authenticated?)
    (let [items (state/all)]
      (spit "/temp/clojure/session.txt" items)
      (page/html5 
        [:head [:title "Session Values"]]
        [:body [:table 
                [:tr [:th "KEY"][:th "VALUE"]]
                (for [[k v] items]
                  [:tr [:td k][:td (with-out-str (prn v))]])]]))
    (respond/forbidden-view ctx)))

(as-method view-session endpoint/endpoints "get-dev-session")
