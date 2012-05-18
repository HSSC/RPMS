;; Provides helper functions for generating certain jquery scripts on the browser.
(ns org.healthsciencessc.rpms2.consent-admin.ui.container
  (use [org.healthsciencessc.rpms2.consent-admin.ui.common]))

;; Define ScrollBox
(defn scrollbox
  [& content]
  [:div.scrollbox content])