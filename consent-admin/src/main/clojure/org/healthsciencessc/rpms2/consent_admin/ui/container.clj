;; Provides helper functions for generating certain jquery scripts on the browser.
(ns org.healthsciencessc.rpms2.consent-admin.ui.container
  (:require [clojure.string :as twine])
  (:use [org.healthsciencessc.rpms2.consent-admin.ui.common]))

;; Defines a ScrollBox container that provides scrollbars when content goes
;; outside it's containers.
(defn scrollbox
  [& content]
  [:div.scrollbox content])

;; Defines a cutbox container that will cut off all content that goes outside
;; it's boundaries.
(defn cutbox
  [& content]
  [:div.cutbox content])

;; Defines a tab control.
(defn- gen-tab-name
  [tab prefix]
  (str prefix (twine/replace (:label tab) " " "-")))

(defn tabcontrol
  [options tabs]
  [(tag-class :div.tabbox (:fill options))
   [:div.tabcontrol
    ;; Generate Labels
    [:ul (map (fn [tab] [:li [:a {:href (gen-tab-name tab "#tab-")} (:label tab)]]) tabs)]
    ;; Generate Content
    (map (fn [tab] [(tag-class :div.tab (:fill options)) {:id (gen-tab-name tab "tab-")} (:content tab)]) tabs)]])