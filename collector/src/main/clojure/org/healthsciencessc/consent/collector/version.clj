(ns org.healthsciencessc.consent.collector.version
  (:require [org.healthsciencessc.consent.collector.text :as text]
            [org.healthsciencessc.consent.collector.ui.content :as uicontent]
            [pliant.configure.props :as props]))

(def config (props/slurp-config "/consent/version.properties"))

(defn build-date
  []
  (:build.date config))

(defn version
  []
  (:version config))

(defn version-label
  []
  (uicontent/block-text-small 
    (text/text :footer.application.label)
    (text/format-text :footer.version.label {:args [(version)]})
    (text/format-text :footer.build-date.label {:args [(build-date)]})))