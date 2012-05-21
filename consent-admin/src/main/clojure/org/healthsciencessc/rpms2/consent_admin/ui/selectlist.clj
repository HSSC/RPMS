(ns org.healthsciencessc.rpms2.consent-admin.ui.selectlist
  (:use [org.healthsciencessc.rpms2.consent-admin.ui.common]))

(defn- selectitemlist
  [{label :label data :data}]
  [:div.selectlistitem.ui-state-default {:data-item (to-attr-value data)} label ])

(defn- selectitem-loop
  [many]
  (for [one many]
    (cond
      (map? one) (selectitemlist one)
      (seq? one) (selectitem-loop one))))

(defn selectlist
  [& items]
  [:div.selectlist.ui-state-default (selectitem-loop items)])
