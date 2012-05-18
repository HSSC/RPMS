(ns org.healthsciencessc.rpms2.consent-admin.ui.form
  (use [org.healthsciencessc.rpms2.consent-admin.ui.form]))

(defn dataform
  [& items]
  [:div.dataform [:form.dataform items]])


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
