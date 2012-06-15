(ns org.healthsciencessc.rpms2.consent-admin.ui.form
  (:use [org.healthsciencessc.rpms2.consent-admin.ui.common]))

(def readonly-props {:disabled true})

(def required-props {:data-required true})

(defn required
  [options props]
  (if (:required options) (merge props required) props))

(defn dataform
  [& items]
  [:div.dataform [:form.dataform items]])

(defn- get-common-props
  [{name :name
    readonly :readonly
    editable :editable}]
  (let [props {:name name}
        disabled-props (if (or (false? editable) readonly) readonly-props {})]
    (merge props disabled-props)))

(defn input-password
  "Generates a text input."
  [{:keys [value label classes] 
    :as field}]
  (let [props (merge {:type :password :name name} (get-common-props field))]
    [(tag-class :div.form-control-wrapper.form-password classes)
     [(tag-class :label.text classes) {:for name} label]
     [(tag-class :input.password classes) props]]))

(defn input-text
  "Generates a text input."
  [{:keys [value label classes] 
    :as field}]
  (let [props (merge {:type :text :value value} (get-common-props field))]
    [(tag-class :div.form-control-wrapper.form-text classes)
     [(tag-class :label.text classes) {:for name} label]
     [(tag-class :input.text classes) props]]))

(defn input-checkbox
  "Generates a checkbox input."
  [{:keys [label classes] 
    :as field}]
  (let [clean (fn [& all] (str (first (remove nil? all))))
        checked-value (clean (:checked-value field) true)
        unchecked-value (clean (:unchecked-value field) false)
        default-checked (first (remove nil? [(:default-checked field) false]))
        value (clean (:value field) (if default-checked checked-value unchecked-value))
        checked (= value checked-value)
        props (merge {:type :checkbox  
               :name name
               :data-checked-value checked-value
               :data-unchecked-value unchecked-value} (get-common-props field))]
  [(tag-class :div.form-control-wrapper.form-checkbox classes)
    [(tag-class :input.checkbox classes) (if checked (assoc props :checked :checked) props)]
    [(tag-class :label.checkbox classes) {:for name}] label]))

(defn input-hidden
  "Generates a hidden data holder."
  [{:keys [value name]
    :as field}]
  [:hidden {:value value :name name}])

(def selected {:selected true})

(defn singleselect
  "Generate a select control that allows only a single value selected"
  [{:keys [items value label blank classes]
    :as field}]
  (let [props (merge {:value value} (get-common-props field))]
    [(tag-class :div.form-control-wrapper.form-select classes)
     [(tag-class :label.single-select classes) {:for name} label]
     [(tag-class :select.single-select classes) props
      ;;(if (and blank (not value)) [:option selected])
      (for [{:keys [label data item]} items]
        (let [props {:value data}
              props (if (= data value) (merge selected props) props)
              props (if item (merge {:data-item (to-attr-value item)} props) props)]
          [:option props label]))]]))
  
(defn multiselect
  [{:keys [label name items classes]
    :as field}]
  ;; wrap this so that it doesn't disappear
  [(tag-class :div.form-control-wrapper.form-multiselect classes)
    [:label {:for name} label]
    [:select {:multiple true :name name}
     (for [{:keys [value label]} items]
       [:option {:value value} label])]])

;; Define the custom controls
(defn relations-list
  [{label :label 
    name :name
    value :value
    classes :classes
    item-label :item-label
    items :items
    delete-url :delete-url}]
  (let [props ()]
    [(tag-class :div.form-control-wrapper.relations-list-wrapper classes)
     [(tag-class :label.relations-list classes) {:for name} label]
     [(tag-class :div.relations-list )]]))

;; Define the generic edit-field methods
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

(defmethod edit-field :singleselect
  [field]
  (singleselect field))

(defmethod edit-field :multiselect
  [field]
  (multiselect field))

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
  ([options fields] (render-fields options fields {}))
  ([options fields record]
    (let [field-mods (or (:fields options) {})
          options (dissoc options :fields)]
      (map #(let [field-name (:name %)
                  field-options (field-mods field-name)]
              (record->editable-field record (merge options % field-options))) fields))))
