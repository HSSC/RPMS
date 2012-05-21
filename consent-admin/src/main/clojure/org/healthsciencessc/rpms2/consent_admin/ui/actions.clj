;; Provides helper functions for generating certain jquery scripts on the browser.
(ns org.healthsciencessc.rpms2.consent-admin.ui.actions
  (:use [org.healthsciencessc.rpms2.consent-admin.ui.common]))

;; Define Methods for Creating an actionsx
(defn actions
  [& actions]
  [:div.actionbox actions])

;;
(defn details-button
  [{url :url params :params label :label}]
  [:div.action.details-action.ui-button.ui-widget.ui-state-default.ui-corner-all.ui-button-text-only 
   {:data-url url  :data-map (to-attr-value params)} [:span.ui-button-text (or label "Details")]])

;;
(defn save-new-button
  [{method :method url :url params :params label :label}]
  [:div.action.save-action.ui-button.ui-widget.ui-state-default.ui-corner-all.ui-button-text-only 
   {:data-method method :data-url url :data-map (to-attr-value params)} [:span.ui-button-text (or label "Save")]])

;;
(defn save-button
  [{method :method url :url params :params label :label}]
  (list
    [:div.action.save-jquery-action.ui-button.ui-widget.ui-state-default.ui-corner-all.ui-button-text-only 
      {:data-method method :data-url url :data-map (to-attr-value params) :data-target "#progress-dialog"}
      [:span.ui-button-text (or label "Save")]]
    [:div#progress-dialog]))

;;
(defn new-button
  [{url :url label :label}]
  [:div.action.new-action.ui-button.ui-widget.ui-state-default.ui-corner-all.ui-button-text-only 
   {:data-url url} [:span.ui-button-text (or label "Add")]])


(defn pop-button
  ([] (pop-button {}))
  ([{params :params label :label}]
  [:div.action.done-action.ui-button.ui-widget.ui-state-default.ui-corner-all.ui-button-text-only 
   {:data-map (to-attr-value params)} [:span.ui-button-text (or label "Done")]]))
