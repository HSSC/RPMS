(ns org.healthsciencessc.rpms2.consent-collector.i18n
  (:use [clojure.tools.logging :only (info)])
  (:require [j18n.core :as j18n]
            [clojure.string :as s]))

(def ^:private my-bundle (java.util.ResourceBundle/getBundle "org/healthsciencessc/rpms2/consent_collector/j18n"))

(defn- make-resource-keyword
  "Creates keyword of the arguments, joined by -.  
  The resource names are often composed of a form name
  and a type (e.g. form1-firstname-label)"
  [parts]
  (->> parts
       (map name)
       (s/join "-")
       (keyword)))

(defn i18n-existing
  "Returns resource string associated with the arguments,
  which are joined by a dash. "
  [a & bs]

  (info "i18n-existing " a " bs " bs )
  (j18n/resource (make-resource-keyword (cons a bs)) my-bundle))

(defn i18n
  "Returns resource string associated with the message.
   If nothing is defined in the resource bundle, prints a message
   and returns the name."
  [a & bs]
  (info "i18n " a " bs " bs )
  (or (apply i18n-existing a bs)
      (let [msg (make-resource-keyword (cons a bs))]
        (info "missing resource definition: " msg)
        (name msg))))
