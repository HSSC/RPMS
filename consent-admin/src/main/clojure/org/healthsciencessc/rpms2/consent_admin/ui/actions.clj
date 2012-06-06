;; Provides helper functions for generating certain jquery scripts on the browser.
(ns org.healthsciencessc.rpms2.consent-admin.ui.actions
  (:use [org.healthsciencessc.rpms2.consent-admin.ui.common]))

;; Define Methods for Creating an actionsx
(defn actions
  [& actions]
  [:div.actionbox actions])

(def action-tag :div.action)

(def jquery-classes :.ui-button.ui-widget.ui-state-default.ui-corner-all.ui-button-text-only)

;; Define Generic Pattern Actions
(def default-confirm 
  {:title "Confirm Action" :message "Are you sure you want to proceed?"})

(defn- confirm-to-attr
  [conf]
  (cond
    (map? conf)
      (to-attr-value (merge default-confirm conf))
    :else
    (to-attr-value (assoc default-confirm :message (str conf)))))

(defn action-separator
  []
  [:div.action-separator])

;; 
(defn ajax-action
  "Generates an action that will make a call via ajax to the server.  Allows for confirmation of action 
prior to calling the server, and the execution of a client-side action when the ajax call is successful"
  [{method :method url :url params :params label :label classes :classes
    confirm :confirm 
    action-on-success :action-on-success
    include-data :include-data}]
  (let [props {:data-method method :data-url url :data-map (to-attr-value params)}
        action-props (if action-on-success {:data-action-on-success action-on-success} {})
        conf-props (if confirm {:data-confirm (confirm-to-attr confirm)} {})
        data-props (if include-data {:data-include-data true} {})]
    [(tag-class action-tag :ajax-action classes jquery-classes) (merge props action-props conf-props data-props)
      [:span.ui-button-text (or label "Trigger")]]))

(defn push-action
  "Generates an action that will push a new request to the PaneManager."
  [{url :url params :params label :label confirm :confirm classes :classes}]
  (let [props {:data-url url :data-map (to-attr-value params)}
        conf-props (if confirm {:data-confirm (confirm-to-attr confirm)} {})]
    [(tag-class action-tag :push-action classes jquery-classes) (merge props conf-props)
      [:span.ui-button-text (or label "Push")]]))

(defn back-action
  "Generates an action that will trigger a 'back' call to the PaneManager."
  ([] (back-action {}))
  ([{params :params label :label classes :classes}]
  [(tag-class action-tag :back-action classes jquery-classes)
   {:data-map (to-attr-value params)} [:span.ui-button-text (or label "Back")]]))

;; Pre-definedH Actions
;;
(defn pop-button
  ([] (back-action {}))
  ([options] (back-action options)))

;; 
(defn new-button
  [options]
  (push-action (merge {:label "Add"} options)))

;;
(defn details-button[options]
  (push-action (merge {:label "Edit" :classes :.detail-action} options)))

;;
(defn delete-button
  [options]
  (ajax-action (merge {:method :delete :label "Delete" :action-on-success ".back-action" :confirm default-confirm} options)))

;;
(defn save-button
  [options]
  (ajax-action (merge {:label "Save" :action-on-success ".back-action" :include-data :true} options)))
