;; Provides helper functions for generating certain jquery scripts on the browser.
(ns org.healthsciencessc.rpms2.consent-admin.ui.actions
  (:use [org.healthsciencessc.rpms2.consent-admin.ui.common]))

;; Define Methods for Creating an actionsx
(defn actions
  [& actions]
  [:div.actionbox actions])

(def jquery-classes :.ui-button.ui-widget.ui-state-default.ui-corner-all.ui-button-text-only)

;; Define Generic Pattern Actions
(def default-confirm 
  {:title "Confirm Action" :message "Are you sure you want to proceed?"})

(def default-verify 
  {:title "Confirm Action" :message "Please provide a user friendly verification message."})

(defn- confirm-to-attr
  [conf]
  (cond
    (map? conf)
      (to-attr-value (merge default-confirm conf))
    :else
    (to-attr-value (assoc default-confirm :message (str conf)))))

(defn- verify-to-attr
  [verify]
  (cond
    (map? verify)
      (to-attr-value (merge default-verify verify))
    :else
    (to-attr-value (assoc default-verify :action (name verify)))))

(defn action-separator
  []
  [:div.action-separator])

(defn get-common-props
  [{confirm :confirm 
    verify :verify}]
  (let [conf-props (if confirm {:data-confirm (confirm-to-attr confirm)} {})
        verify-props (if verify {:data-verify (verify-to-attr verify)} {})]
    (merge conf-props verify-props)))

;; 
(defn ajax-action
  "Generates an action that will make a call via ajax to the server.  Allows for confirmation of action 
prior to calling the server, and the execution of a client-side action when the ajax call is successful"
  [{method :method url :url params :params label :label classes :classes
    action-on-success :action-on-success
    include-data :include-data
    :as options}]
  (let [props {:data-method method :data-url url :data-map (to-attr-value params)}
        action-props (if action-on-success {:data-action-on-success action-on-success} {})
        common-props (get-common-props options)
        data-props (if include-data {:data-include-data true} {})]
    [(tag-class :div jquery-classes classes :.ajax-action.action) (merge props action-props data-props common-props)
      [:span.ui-button-text (or label "Trigger")]]))

(defn push-action
  "Generates an action that will push a new request to the PaneManager."
  [{url :url params :params label :label classes :classes
    :as options}]
  (let [props {:data-url url :data-map (to-attr-value params)}
        common-props (get-common-props options)]
    [(tag-class :div classes jquery-classes :.push-action.action) (merge props common-props)
      [:span.ui-button-text (or label "Push")]]))

(defn back-action
  "Generates an action that will trigger a 'back' call to the PaneManager."
  ([] (back-action {}))
  ([{params :params label :label classes :classes}]
  [(tag-class :div classes jquery-classes :.back-action.action)
   {:data-map (to-attr-value params)} [:span.ui-button-text (or label "Back")]]))

;; Pre-definedH Actions
;; 
(defn new-action
  [options]
  (push-action (merge {:label "Add"} options)))

;;
(defn details-action
  [options]
  (push-action (merge {:label "Details/Edit" :classes :.detail-action} options)))

;;
(defn delete-action
  [options]
  (ajax-action (merge {:method :delete :label "Delete" :action-on-success ".back-action" :confirm default-confirm} options)))

;;
(defn save-action
  [options]
  (ajax-action (merge {:method :post :label "Save" :action-on-success ".back-action" :include-data :true} options)))

;;
(defn create-action
  [options]
  (ajax-action (merge {:method :put :label "Create" :action-on-success ".back-action" :include-data :true} options)))


;; Define List Actions

(defn push-list-action
  "Generates an action that will push a new request to the PaneManager."
  [{url :url params :params label :label confirm :confirm classes :classes}]
  (let [props {:data-url url :data-map (to-attr-value params)}
        conf-props (if confirm {:data-confirm (confirm-to-attr confirm)} {})]
    [(tag-class :div jquery-classes :.push-listaction.listaction classes) (merge props conf-props)
      [:span.ui-button-text (or label "Push")]]))

(defn ajax-list-action
  "Generates an action that will push a new request to the PaneManager."
  [{url :url params :params label :label confirm :confirm classes :classes
    action-on-success :action-on-success
    include-data :include-data}]
  (let [props {:data-url url :data-map (to-attr-value params)}
        action-props (if action-on-success {:data-action-on-success action-on-success} {})
        conf-props (if confirm {:data-confirm (confirm-to-attr confirm)} {})
        data-props (if include-data {:data-include-data true} {})]
    [(tag-class :div jquery-classes :ajax-listaction.listaction classes) (merge props conf-props action-props data-props)
      [:span.ui-button-text (or label "Push")]]))

(defn add-list-action
  "Generates an action that will push a new request to the PaneManager."
  [options]
  (push-list-action (merge options {:label "Add"})))

(defn edit-list-action
  "Generates an action redirect "
  [options]
  (push-list-action (merge options {:label "Edit"})))

(defn delete-list-action
  "Generates an action that will delete the selected item."
  [options]
  (ajax-list-action (merge options {:method :post :label "Delete" :confirm {:title "Confirm Delete" :message "Are you sure you want to delete the selected item?"}})))
