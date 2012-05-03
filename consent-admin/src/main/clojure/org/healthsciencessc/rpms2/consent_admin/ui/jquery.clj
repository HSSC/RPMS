;; Provides helper functions for generating certain jquery scripts on the browser.
(ns org.healthsciencessc.rpms2.consent-admin.ui.jquery
  (require [hiccup.element :as element]))


(defn center-on
  "Creates a script that will center one element within another."
  [positionable parent]
  (element/javascript-tag 
    (str "$(function(){$('" (name positionable) "').position({my: 'center', at: 'center', of: '" (name parent) "'});});")))