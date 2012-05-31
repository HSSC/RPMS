(ns org.healthsciencessc.rpms2.consent-admin.ui.form
  (:use [org.healthsciencessc.rpms2.consent-admin.ui.common]))

(defn dataform
  [& items]
  [:div.dataform [:form.dataform items]])

(defn input-password
  "Generates a text input."
  [{value :value name :name label :label classes :classes}]
  [(tag-class :div.form-control-wrapper.form-password classes)
    [(tag-class :label.text classes) {:for name} label]
    [(tag-class :input.password classes) {:type :password
                      :name name}]])

(defn input-text
  "Generates a text input."
  [{value :value name :name label :label classes :classes}]
  [(tag-class :div.form-control-wrapper.form-text classes)
    [(tag-class :label.text classes) {:for name} label]
    [(tag-class :input.text classes) {:type :text :value value :name name}]])

(defn input-checkbox
  "Generates a checkbox input."
  [{name :name checked-value :checked-value unchecked-value :unchecked-value
    default-checked :default-checked value :value label :label classes :classes}]
  (let [checked-value (or checked-value true)
        unchecked-value (or unchecked-value false)
        default-checked (or default-checked false)
        value (or value (if default-checked checked-value unchecked-value))
        checked (= value checked-value)
        props {:type :checkbox  :name name
                      :data-checked-value checked-value
                      :data-unchecked-value unchecked-value
                      :value value}]
  [(tag-class :div.form-control-wrapper.form-checkbox classes)
    [(tag-class :input.checkbox classes) (if checked (assoc props :checked :checked) props)]
    [(tag-class :label.checkbox classes) {:for name}] label]))

(defn input-hidden
  "Generates a hidden data holder."
  [{value :value name :name}]
  [:hidden {:value value :name name}])

(defn combobox
  "Generate a combobox"
  [{name :name
    items :items 
    classes :classes}]
  [(tag-class :div.form-control-wrapper.form-select classes)
    [(tag-class :select.single-select classes) {:name name}
      (for [{:keys [label data selected]} items]
        [:option {:value data
                  :selected selected} label])]])

(defmulti edit-field :type)

(defmethod edit-field :text
  [field]
  (input-text field))

(defmethod edit-field :password
  [field]
  (input-password field))

(defmethod edit-field :checkbox
  [field]
  (input-checkbox field))

(defmethod edit-field :default
  [field]
  (input-text field))

(defn record->editable-field
  "Takes an arbitrary record (map) and
  a list of maps with name, label, and optional type"
  [record {field-kw :name
           field-type :type
           :as field}]
  (let [field-val (get record field-kw)
        field-type (or field-type :text)]
    (edit-field (assoc field
                       :value field-val
                       :type field-type))))

(defn render-fields
  ([fields] (render-fields fields {}))
  ([fields record]
    (map record->editable-field 
         (repeat record)
         fields)))
