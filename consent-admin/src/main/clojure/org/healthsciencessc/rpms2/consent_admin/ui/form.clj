(ns org.healthsciencessc.rpms2.consent-admin.ui.form
  (:use [org.healthsciencessc.rpms2.consent-admin.ui.form]))

(defn dataform
  [& items]
  [:div.dataform [:form.dataform items]])

(defn input-password
  "Generates a text input."
  [{value :value name :name label :label}]
  (list
    [:label.text {:for name} label]
    [:input.password {:type :password
                      :name name}]))

(defn input-text
  "Generates a text input."
  [{value :value name :name label :label}]
  (list
    [:label.text {:for name} label]
    [:input.text {:type :text :value value :name name}]))

(defn input-checkbox
  "Generates a text input."
  [{checked :checked name :name label :label}]
  (list
    [:input.checkbox {:type :checkbox :checked checked :name name}]
    [:label.checkbox {:for name}] label))

(defn input-hidden
  "Generates a hidden data holder."
  [{value :value name :name}]
  [:hidden {:value value :name name}])

(defn combobox
  "Generate a combobox"
  [{name :name
    items :items}]
  [:select {:name name}
     (for [{:keys [label data selected]} items]
       [:option {:value data
                 :selected selected} label])])

(defmulti edit-field :type)

(defmethod edit-field :text
  [fld]
  (input-text fld))

(defmethod edit-field :password
  [fld]
  (input-password fld))

(defmethod edit-field :default
  [fld]
  (input-text fld))

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
