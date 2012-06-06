(ns org.healthsciencessc.rpms2.consent-admin.ui.selectlist
  (:use [org.healthsciencessc.rpms2.consent-admin.ui.common]))

(defn- selectitemlist
  [options {label :label data :data}]
  (let [props {:data-item (to-attr-value data)}
        action-props (if (:action options) {:data-action (:action options)} {})]
    [:div.selectlistitem.ui-state-default (merge props action-props) label ]))

(defn selectlist
  [options & items]
  [:div.selectlist.ui-state-default 
   (for [item (flatten items)]
     (selectitemlist options item))])
