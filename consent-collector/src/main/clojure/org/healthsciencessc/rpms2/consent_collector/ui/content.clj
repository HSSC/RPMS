(ns org.healthsciencessc.rpms2.consent-collector.ui.content
  (:require [clojure.data.json :as json]))


(defn datalistview
  "Creates a content view that has a list of items on the left that show details on the right when selected."
  [options detail items]
  (let [selecttheme (or (:selecttheme options) :b)
        theme (or (:theme options) :c)
        dividertheme (or (:theme options) :d)
        target (or (:target options) (System/currentTimeMillis))]
    [:div.datalistview {:data-autoselect (or (:autoselect options) "none")}
     [:div.datadetail detail] 
     [:div.datalist 
      [:ul {:data-role :listview :data-theme theme :data-dividertheme dividertheme :data-selecttheme selecttheme}
       (for [{:keys [data label sublabel] :as item} items] 
         [:li [:a.action-datalistviewitem {:data-item (json/json-str data)} 
               [:div.label label]
               [:div.sublabel sublabel]]])]]]))

(defn block-text
  ([text1] [:div.blocktext.blocktext-1-1 text1])
  ([text1 text2] (list [:div.blocktext.blocktext-1-2 text1]
                       [:div.blocktext.blocktext-2-2 text2])))

(defn block-text-small
  ([text1] [:div.blocktextsmall.blocktextsmall-1-1 text1])
  ([text1 text2] (list [:div.blocktextsmall.blocktextsmall-1-2 text1]
                       [:div.blocktextsmall.blocktextsmall-2-2 text2]))
  ([text1 text2 text3] (list [:div.blocktextsmall.blocktextsmall-1-3 text1]
                             [:div.blocktextsmall.blocktextsmall-2-3 text2]
                             [:div.blocktextsmall.blocktextsmall-3-3 text3])))

(defn paragraph
  [& elements]
  [:p elements])

(defn paragraphs
  [& elements]
  (map (fn [e] [:p e]) elements))
