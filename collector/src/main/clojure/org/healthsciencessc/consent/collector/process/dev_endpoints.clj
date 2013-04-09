(ns org.healthsciencessc.consent.collector.process.dev-endpoints
  (:refer-clojure :exclude [root])
  (:require [org.healthsciencessc.consent.client.whoami :as whoami]
            [org.healthsciencessc.consent.collector.respond :as respond]
            [org.healthsciencessc.consent.collector.state :as state]
            [org.healthsciencessc.consent.collector.text :as text]
            [org.healthsciencessc.consent.collector.ui.action :as action]
            [org.healthsciencessc.consent.collector.ui.form :as form]
            [org.healthsciencessc.consent.collector.ui.layout :as layout]
            [hiccup.page :as page]
            [pliant.webpoint.request :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))


;; Register The Select Lockcode View
#_(defprocess view-session
  "Creates a view of what is in the server session."
  [ctx]
  (if (whoami/identified?)
    (let [items (state/all)]
      (spit "/temp/clojure/session.txt" items)
      (page/html5 
        [:head [:title "Session Values"]]
        [:body [:table 
                [:tr [:th "KEY"][:th "VALUE"]]
                (for [[k v] items]
                  [:tr [:td k][:td (with-out-str (prn v))]])]]))
    (respond/forbidden-view ctx)))

#_(as-method view-session endpoint/endpoints "get-dev-session")
