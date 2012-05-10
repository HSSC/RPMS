;; Provides the configuration of the protocol managemant UIs.
(ns org.healthsciencessc.rpms2.consent-admin.process.protocol
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-admin.ui.layout :as layout]
            [org.healthsciencessc.rpms2.consent-domain.runnable :as runnable]
            [hiccup.core :as hcup])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))


(defn view-protocol-location
  ""
  [ctx]
  (layout/render ctx "This Is The First Pane, Bob"
                 "Some bogus text for the first pane."
                 [:input {:type :button :value "Go To Protocol" :onclick "PaneManager.push('/view/protocol', {}, {})"}]
                 [:input {:type :button :value "Reset" :onclick "PaneManager.reset()"}]))

(defn view-protocol
  ""
  [ctx]
  (layout/render ctx "Bob!  Second Pane Here!"
                 "Some bogus text for the second pane."
                 [:input {:type :button :value "Go To Version" :onclick "PaneManager.push('/view/protocol/version', {}, {})"}]
                 [:input {:type :button :value "Pop This Mutha" :onclick "PaneManager.pop({})"}]))

(defn view-protocol-version
  ""
  [ctx]
  (layout/render ctx "Bob!  Second Pane Here!"
                 "Some bogus text for the second pane."
                 [:input {:type :button :value "Pop This Mutha" :onclick "PaneManager.pop({})"}]))

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
   
   ;; Generates the view for a specific protocol version.
   {:name "get-view-protocol-version"
    :runnable-fn (constantly true)
    :run-fn view-protocol-version}
   ])

(process/register-processes (map #(DefaultProcess/create %) process-defns))
