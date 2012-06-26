;; Provides helper functions for generating certain jquery scripts on the browser.
(ns org.healthsciencessc.rpms2.consent-admin.ui.container
  (:require [clojure.string :as twine]
            [hiccup.page :as page])
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

;; Defines a cutbox container that will cut off all content that goes outside
;; it's boundaries.
(defn designer
  [options]
  (let [protocol-version (:protocol-version options)
        editable (if (:editable options) true false)
        props {:data-protocol (to-attr-value protocol-version) :data-editable editable}
        url-props (if editable {:data-url (:url options)
                                :data-params (:params options)} {})]
    (list
      [:div#consent-designer (merge props url-props)]
      (page/include-js "/js/consent-designer.js"))))


;; Defines a tab control.
(defn- gen-tab-name
  [tab prefix]
  (str prefix (twine/replace (:label tab) " " "-")))

(defn tabcontrol
  [options tabs]
  (let [id (or (:id options) (str "tabcontrol-" (count tabs)))
        tctag (keyword (str "div#" id ".tabcontrol"))] 
    [(tag-class :div.tabbox (:fill options))
     [tctag
      ;; Generate Labels
      [:ul (map (fn [tab] [:li [:a {:href (gen-tab-name tab "#tab-")} (:label tab)]]) tabs)]
      ;; Generate Content
      (map (fn [tab] [(tag-class :div.tab (:fill options)) {:id (gen-tab-name tab "tab-")} (:content tab)]) tabs)]]))