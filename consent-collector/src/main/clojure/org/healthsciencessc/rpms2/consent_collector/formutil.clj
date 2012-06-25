(ns org.healthsciencessc.rpms2.consent-collector.formutil
  "General purpose helpers when manipulating the forms."
  (:require [hiccup
               [page :as hpage]
               [element :as helem]])
  (:require [ring.util.response :as ring])
  (:require [org.healthsciencessc.rpms2.consent-collector.mock :as mock])
  (:require [org.healthsciencessc.rpms2.consent-collector.formutil :as formutil])
  (:use [sandbar.stateful-session :only [session-get session-put! session-delete-key! destroy-session! flash-get flash-put!]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [clojure.string :only (replace-first join)])
  (:use [clojure.pprint])

  (:use [org.healthsciencessc.rpms2.consent-domain.tenancy :only [label-for-location label-for-protocol]])
  (:use [org.healthsciencessc.rpms2.consent-collector.debug :only [debug! pprint-str]])
  (:use [org.healthsciencessc.rpms2.consent-collector.config :only [config]])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only (i18n i18n-existing
                                                                       i18n-label-for
                                                                       i18n-placeholder-for)]))

(defn list-widgets-in-form
  "Lists names of all widgets in the form."
  [form]
  (->> form
       :contains        ; pages
       (map :contains)  ; sections
       flatten
       (map :contains)  ; widgets
       flatten
       (map :name)))

(defn list-pages-in-form
  "Lists names of all pages in the form."
  [form]
  (->> form
       :contains        ; pages
       (map :name)))

(defn find-widget-in-form
  "Returns the named widget in the form."
  [form nm]
  (->> form
       :contains        ; pages
       (map :contains)  ; sections
       flatten
       (map :contains)  ; widgets
       flatten
       (filter #(= (:name %) nm))
       first)) 




(defn find-page-in-form
  "Returns the named page in the form."
  [form nm]
  (->> form
       :contains        ; pages
       (filter #(= (:name %) nm))
       first)) 

(defn find-widget-in-page
  "Returns the named widget in the page."
  [page nm]
  (->> page
       :contains        ; sections
       flatten
       (map :contains)  ; widgets
       flatten
       (filter #(= (:name %) nm))
       first)) 


(defn find-policy-in-page
  "Returns the named widget in the page."
  [page nm]
  (->> page
       :contains        ; sections
       flatten
       (map :contains)  ; widgets
       flatten
       (filter #(and (= (:policy %) nm)
                     (or (= (:type %) "policy-choice-buttons")
                         (= (:type %) "policy-button")
                         (= (:type %) "policy-checkbox")))))) 


(defn find-widget-in-form-on-page
  "Returns the named widget in the form which is contained on the page named 'page-nm'.
  Because there can be multiple pages with the same widget name (eg. one for the
  regular flow, and one for the review page)."
  [form w-nm page-nm]
  (let [page (find-page-in-form form page-nm)]
        (find-widget-in-page page w-nm) ))

