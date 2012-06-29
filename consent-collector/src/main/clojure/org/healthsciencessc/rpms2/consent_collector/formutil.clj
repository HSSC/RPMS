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
  (let [branch? #(seq (:contains %))]
    (tree-seq branch? :contains form)))

(defn signature-widget? [w]
  (= "signature" (:type w)))

(defn policy-widget? [{type :type}]
  (or (= type "policy-choice-buttons")
      (= type "policy-button")
      (= type "policy-checkbox")))

(defn find-policy-in-page
  "Returns the named widget in the page or section or form"
  [page nm]
  (first (filter #(and (policy-widget? %)
                       (= nm (:name %)))
                 (list-widgets-in-form page))))

(defn widget-by-guid [form guid]
  (let [widget-map (into {} (for [{id :id :as widget}
                                  (list-widgets-in-form form)]
                              [id widget]))]
    (get widget-map guid)))

(defn widget-properties [w]
  (into {} (for [{:keys [key value]} (:properties w)]
             [(keyword key) value])))

(defn widget-props-localized-impl [w lang-id]
  (let [props (:properties w)
        localized-props (->> props 
                          (filter #(= lang-id (:id (:language %)))))
        normalprops (remove :language props)]
    (into {} (for [{:keys [key value]} (concat props localized-props)]
               [(keyword key) value]))))

(defn widget-props-localized [w]
  (widget-props-localized w (session-get :selected-language)))
