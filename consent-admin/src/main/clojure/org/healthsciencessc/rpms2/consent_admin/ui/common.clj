;; Provides helper functions for generating certain jquery scripts on the browser.
(ns org.healthsciencessc.rpms2.consent-admin.ui.common
  (:require [clojure.data.json :as json]))

(defn to-attr-value
  "Turns a clojure value into a valid value for an element attribute."
  [v]
  (cond
    (map? v) (json/json-str v)
    :else
    v))

(defn dotit
  [v]
  (let [s (name v)]
    (if (.startsWith s ".")
      s 
      (str "." s))))

(defn smooshkw
  [& words]
  (keyword (apply str (map name words))))
        
(defn tag-class
  ([tag] tag)
  ([tag & classes]
    (let [cls (remove nil? (flatten classes))]
    (if (not= 0 (count cls))
      (apply smooshkw (cons tag (map dotit (flatten classes))))
      tag))))