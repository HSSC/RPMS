(ns org.healthsciencessc.consent.collector.ui.form
  (:require [hiccup.element :as helement])
  (:use     [pliant.process :only [defprocess]]))


(defn dataform
  "Create a form container that will not allow the normal submit."
  [{:keys [url method ajax] :as options} & items]
  [:form {:action url 
          :data-ajax "false" 
          :data-theme "a" 
          :method method
          :onsubmit "return false;"} items])

(defprocess input-wrapper
  [field & items]
  (if (:contain field)
    [:div {:data-role "fieldcontain"} items]
    items))

(defn wrap-controlgroup
  [wrapper fields]
  (input-wrapper wrapper [:fieldset {:data-role "controlgroup"} fields]))

(defn input-disabled
  [attributes field]
  (if (:disabled field)
    (merge attributes {:data-disabled "true"})
    attributes))

(defn input-id
  ([field given-name] (input-id field given-name nil))
  ([field given-name suffix] 
    (str (name given-name) (:suffix field) suffix)))

(defn input-hidden
  "Generates a hidden input."
  [{:keys [value name label] :as field}]
  (let [id (input-id field name)]
    [:input {:id id :name name :value value :type "hidden"}]))

(defn input-password
  "Generates a password input."
  [{:keys [value name label placeholder autofocus] :as field}]
  (let [id (input-id field name)]
    (input-wrapper 
      field 
      (if label [:label {:for id} label])
      [:input (input-disabled 
                {:id id :name name :value value :type "password" :placeholder placeholder :autofocus autofocus} field)])))

(defn input-username
  "Generates a text input for the user name."
  [{:keys [value name label placeholder autofocus] :as field}]
  (let [id (input-id field name)]
    (input-wrapper 
      field  
      (if label [:label {:for id} label])
      [:input (input-disabled 
                {:id id :name name :value value :type "text" :placeholder placeholder :autofocus autofocus} field)])))

(defn input-text
  "Generates a text input."
  [{:keys [value name label placeholder autofocus] :as field}]
  (let [id (input-id field name)]
    (input-wrapper 
      field  
      (if label [:label {:for id} label])
      [:input (input-disabled 
                {:id id :name name :value value :type "text" :placeholder placeholder :autofocus autofocus} field)])))

(defn input-date
  "Generates a text input with date helper."
  [{:keys [value name label placeholder autofocus] :as field}]
  (let [id (input-id field name)]
    (input-wrapper 
      field  
      (if label [:label {:for id} label])
      [:input (input-disabled 
                {:id id :name name :value value :type "date" :placeholder placeholder :autofocus autofocus} field)])))

(defn input-number
  "Generates a text input that only accepts numbers."
  [{:keys [value name label placeholder autofocus] :as field}]
  (let [id (input-id field name)]
    (input-wrapper 
      field  
      (if label [:label {:for id} label])
      [:input (input-disabled 
                {:id id :name name :value value :type "number" :pattern "[0-9]*" :placeholder placeholder :autofocus autofocus} field)])))

(defn radio-checked
  [field value attributes]
  (if (= value (:value field))
    (merge attributes {:checked "true"})
    attributes))

(defn input-radio
  "Generates a radio button."
  [{:keys [items name label autofocus] :as field}]
  (input-wrapper field
                 [:fieldset {:data-role "controlgroup"}
                  [:legend label]
                  (doall 
                    (map-indexed 
                      (fn [idx {:keys [value label] :as item}]
                        (let [id (input-id field name idx)
                              attributes (input-disabled 
                                           {:id id :name name :value value :type "radio" :autofocus autofocus} field)]
                          (list
                            [:input (radio-checked field value attributes)]
                            [:label {:for id} label]))) items))]))

(defn checkbox-checked
  [attributes {:keys [value checked-value] :as field}]
  (if (or (and (not= value nil) 
               (= value checked-value))
          (and (nil? value) 
               (:checked field)))
    (merge attributes {:checked true})
    attributes))

(defn checkbox-value
  [value]
  (cond
    (true? value) "true"
    (false? value) "false"
    :else value))

(defn input-checkbox
  "Generates a checkbox control."
  [{:keys [name label checked-value unchecked-value include-only within-custom autofocus] :as field}]
  (let [id (input-id field name)
        attributes {:id id
                    :type :checkbox 
                    :name name
                    :autofocus autofocus
                    :data-checked-value (checkbox-value checked-value)
                    :data-unchecked-value (checkbox-value unchecked-value)
                    :data-include-only include-only
                    :data-within-custom within-custom}]
    (input-wrapper field
                   [:input (-> attributes
                             (checkbox-checked field)
                             (input-disabled field))]
                   [:label {:for id} label])))

(defn selected
  [field value attributes]
  (if (= value (:value field))
    (merge attributes {:selected "true"})
    attributes))

(defn select-one
  "Generates a select control."
  [{:keys [items value name blank label autofocus] :as field}]
  (let [id (input-id field name)]
    (input-wrapper 
      field 
      [:label.select {:for id} label]
      [:select (input-disabled 
                  {:id id :name name :value value :type "number" :pattern "[0-9]*" :autofocus autofocus} field)
       (if blank [:option (selected field nil {:value ""}) ""])
       (for [{:keys [value label] :as item} items]
         [:option (selected field value {:value value}) label])])))


(defn custom-checklist
  "Generates a list of checkable items to allow for many values in a single field."
  [{:keys [items name label] :as field}]
  (let [id (input-id field name)]
    (input-wrapper 
      field
      [:fieldset.custom-input {:data-role "controlgroup"
                               :data-name (clojure.core/name name)
                               :data-type "checklist"}
       [:legend label]
       (for [{:keys [value label checked disabled] :as item} items]
         (input-checkbox {:name (str (clojure.core/name name) "_" value)
                          :label label
                          :checked-value value
                          :checked checked
                          :disabled disabled
                          :within-custom "true"}))])))


(defmulti wrap-fields (fn [w fs] (:wrapper w)))

(defmethod wrap-fields :controlgroup
  [wrapper fields]
  (wrap-controlgroup wrapper fields))

;; Define the generic edit-field methods
(defmulti edit-field :type)

(defmethod edit-field :hidden
  [field]
  (input-hidden field))

(defmethod edit-field :username
  [field]
  (input-username field))

(defmethod edit-field :password
  [field]
  (input-password field))

(defmethod edit-field :date
  [field]
  (input-date field))

(defmethod edit-field :number
  [field]
  (input-number field))

(defmethod edit-field :radio
  [field]
  (input-radio field))

(defmethod edit-field :checkbox
  [field]
  (input-checkbox field))

(defmethod edit-field :select-one
  [field]
  (select-one field))

(defmethod edit-field :checklist
  [field]
  (custom-checklist field))

(defmethod edit-field :default
  [field]
  (input-text field))

(defn record->editable-field
  "Takes an arbitrary record (map) and
  a list of maps with name, label, and optional type"
  [record {field-kw :name
           field-type :type
           parser :parser
           :as field}]
  (let [field-val (get record field-kw)
        field-val (if parser (parser field-val) field-val)
        field-type (or field-type :text)]
    (edit-field (assoc field
                       :value field-val
                       :type field-type))))

(defn render-fields
  ([options fields] 
    (render-fields options fields {}))
  ([options fields record]
    (render-fields (dissoc options :fields) fields record (or (:fields options) {})))
  ([options fields record field-mods]
    (map #(let [field-name (:name %)
                field-options (field-mods field-name)]
            (if (:wrapper %)
              (wrap-fields % (render-fields options (:fields %) record field-mods))
              (record->editable-field record (merge options % field-options)))) fields)))
