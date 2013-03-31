(ns org.healthsciencessc.consent.manager.ui.list
  (:require [clojure.string :as twine]
            [org.healthsciencessc.consent.manager.ui.actions :as actions])
  (:use [org.healthsciencessc.consent.manager.ui.common]))

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
    (map (fn [item]
           [:div.actionlistitem {:data-item (to-attr-value (:data item)) } (:label item)]) items)]
   [:div.actionlistactions 
    (if (:editable options)
      (list (if (:add-url options) 
              (actions/add-list-action {:url (:add-url options) :params (:add-params options)}))
            (if (:edit-url options) 
              (actions/edit-list-action {:url (:edit-url options) :params (:edit-params options)}))
            (if (:delete-url options) 
              (actions/delete-list-action {:url (:delete-url options) :params (:delete-params options)}))))]])

(defn- select-row-data
  [item]
  (if (:data item) {:data-item (to-attr-value (:data item))} {}))

(defn- select-row
  "Generates a selectable datarow for a table"
  [item]
  [(tag-class :tr.selectlist) (select-row-data item)
    (for [label (:labels item)]
      [:td.selectlist label])])

(defn- select-header
  "Generates a header row for a select table."
  [options]
  [(tag-class :tr.selectlist)
    (for [headers (:headers options)]
      [:th.selectlist headers])])

(defn select-table
  "Generates a select list based off of a table."
  [options items]
  [(tag-class :table.selectlist) 
   [:thead (select-header options)]
   [:tbody (for [item items]
             (select-row item))]])

(defn- labelled-item
  "Generates a selectable datarow for a table"
  [item]
  (list
    [:td.labelledlist-label (:label item)][:td.labelledlist-data (:data item)]))

(defn- labelled-groups
  [items columns]
  (let [indexed-items (map vector (range) items)
        groups (group-by #(mod (first %) columns) indexed-items)]
  (for [column (range columns)]
    (map #(last %) (groups column)))))

(defn labelled-list
  "Generates a list that has labels and data side by side."
  [options items]
  (let [title (:title options)
        columns (or (:columns options) 1)
        groups (labelled-groups items columns)]
    [(tag-class :div.labelledlist)
     (if title [:span title])
     [(tag-class :table.labelledlist)
      [:tr (for [group groups]
             [:td 
              [:table 
               (for [item group] 
                 [:tr (labelled-item item)])]])]]]))
     