;; Provides helper functions for generating certain jquery scripts on the browser.
(ns org.healthsciencessc.rpms2.consent-admin.ui.common
  (:require [clojure.data.json :as json]))

(defn to-attr-value
  "Turns a clojure value into a valid value for an element attribute."
  [v]
  (cond
    (map? v) (json/json-str v)
    (vector? v) (json/json-str v)
    (seq? v) (json/json-str (vec v))
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
      (apply smooshkw (cons tag (map dotit cls)))
      tag))))


;; Functions for defining common behaviors and options.
(defn options
  "Accepts a variable number of maps and functions that operate on them to put together a single options map."
  [& opts]
  (loop [maps (merge-with cons (filter map? opts))
         functions (filter fn? opts)]
    (let [f (first functions)
          n (next functions)]
      (if (empty? functions)
        maps
        (recur (f maps) n)))))
        
(defn disabled
  ([] (disabled {}))
  ([opts] (merge opts {:disabled true})))

(defn scrollable
  ([] (scrollable {}))
  ([opts] (merge opts {:scrollable :.scroll})))

(defn fill
  ([] (fill {}))
  ([opts] (merge opts {:fill :.fill})))

(defn fill-down
  ([] (fill-down {}))
  ([opts] (merge opts {:fill :.fill-down})))

(defn fill-right
  ([] (fill-right {}))
  ([opts] (merge opts {:fill :.fill-rigth})))
