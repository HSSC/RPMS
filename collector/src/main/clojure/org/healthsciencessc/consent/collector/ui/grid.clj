(ns org.healthsciencessc.consent.collector.ui.grid)

(def mappings {1 [:div.ui-block-a]
               2 [:div.ui-block-a :div.ui-block-b]
               3 [:div.ui-block-a :div.ui-block-b :div.ui-block-c]
               4 [:div.ui-block-a :div.ui-block-b :div.ui-block-c :div.ui-block-d]
               5 [:div.ui-block-a :div.ui-block-b :div.ui-block-c :div.ui-block-d :div.ui-block-e]})

(defn block
  [items partition-at]
  (doall 
    (for [group (partition partition-at items)]
      (for[item (partition 2 (interleave (mappings partition-at) group))]
        [(first item) (last item)]))))

(defn gridify
  [options & items]
  (let [cols (or (:columns options) (count items))]
    (cond
      (= cols 1) [:div.ui-grid-solo (block items 1)]
      (= cols 2) [:div.ui-grid-a (block items 2)]
      (= cols 3) [:div.ui-grid-b (block items 3)]
      (= cols 4) [:div.ui-grid-c (block items 4)]
      (>= cols 5) [:div.ui-grid-d (block items 5)]
      :else items)))
