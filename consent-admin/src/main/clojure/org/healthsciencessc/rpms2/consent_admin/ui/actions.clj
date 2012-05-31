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
   {:data-url url  :data-map (to-attr-value params)} [:span.ui-button-text (or label "Edit")]])

;;
(defn save-button
  [{method :method url :url params :params label :label}]
  [:div.action.save-action.ui-button.ui-widget.ui-state-default.ui-corner-all.ui-button-text-only 
   {:data-method method :data-url url :data-map (to-attr-value params)} [:span.ui-button-text (or label "Save")]])

;;
(defn new-button
  [{url :url params :params label :label}]
  [:div.action.new-action.ui-button.ui-widget.ui-state-default.ui-corner-all.ui-button-text-only 
   {:data-url url :data-map (to-attr-value params)} [:span.ui-button-text (or label "Add")]])

;;
(defn delete-button
  [{url :url params :params label :label}]
  [:div.action.delete-action.ui-button.ui-widget.ui-state-default.ui-corner-all.ui-button-text-only 
   {:data-url url  :data-map (to-attr-value params)} [:span.ui-button-text (or label "Delete")]])

(defn pop-button
  ([] (pop-button {}))
  ([{params :params label :label}]
  [:div.action.done-action.ui-button.ui-widget.ui-state-default.ui-corner-all.ui-button-text-only 
   {:data-map (to-attr-value params)} [:span.ui-button-text (or label "Done")]]))
