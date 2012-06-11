(ns org.healthsciencessc.rpms2.consent-admin.ui.selectlist
  (:require [clojure.string :as twine]
            [org.healthsciencessc.rpms2.consent-admin.ui.actions :as actions])
  (:use [org.healthsciencessc.rpms2.consent-admin.ui.common]))

;; Define a list of items that can be selected.
(defn- selectitemlist
  [options {label :label data :data}]
  (let [props {:data-item (to-attr-value data)}
        action-props (if (:action options) {:data-action (:action options)} {})]
    [:div.selectlistitem.ui-state-default (merge props action-props) label ]))

(defn selectlist
  [options & items]
  [(tag-class :div :.ui-state-default.selectlist (or (:fill options) :fill-down))
   (for [item (flatten items)]
     (selectitemlist options item))])

;; Define a list of items that allow the item to delete itself.
(defn- gen-tab-name
  [section prefix]
  (str prefix (twine/replace (:label section) " " "-")))

(defn actionlist
  "Generates an list of selectable items that have actions associated with them."
  [options items]
  [(tag-class :div.actionlist (:fill options))
   [(tag-class :div.actionlistitems.fill-rightto (if (:fill options) :fill-down)) {:data-fillto :.actionlistactions :data-fillbuffer 5}
    [:div.actionlistitem "Test 1"]
    [:div.actionlistitem "Test 2"]
    [:div.actionlistitem "Test 3"]
    [:div.actionlistitem "Test 4"]
    (map (fn [item]
           [:div.actionlistitem {:data-item (to-attr-value (:data item)) } (:label item)]) items)]
   [:div.actionlistactions 
    (if (:editable options)
      (list (actions/add-list-action {:url (:add-url options "") :params (:add-params options "{}")})
            (actions/edit-list-action {:url (:edit-url options "") :params (:edit-params options "{}")})
            (actions/delete-list-action {:url (:delete-url options "") :params (:delete-params options "{}")}) ))]])
