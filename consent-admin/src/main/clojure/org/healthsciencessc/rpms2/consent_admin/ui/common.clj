;; Provides helper functions for generating certain jquery scripts on the browser.
(ns org.healthsciencessc.rpms2.consent-admin.ui.common
  (require [clojure.data.json :as json]))

(defn to-attr-value
  "Turns a clojure value into a valid value for an element attribute."
  [v]
  (cond
    (map? v) (json/json-str v)
    :else
    v))