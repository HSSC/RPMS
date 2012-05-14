(ns org.healthsciencessc.rpms2.consent-collector.i18n
  (:use [clojure.tools.logging :only (debug info)])
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

  (j18n/resource (make-resource-keyword (cons a bs)) my-bundle))

(defn i18n
  "Returns resource string associated with the message.
   If nothing is defined in the resource bundle, prints a message
   and returns the name."
  [a & bs]
  (or (apply i18n-existing a bs)
      (let [msg (make-resource-keyword (cons a bs))]
        (info "missing resource definition: " msg " a " a  " bs " bs )
        (name msg))))

(defn i18n-for
  "If there's a form-specific label for the field, use that. 
  Otherwise, use the generic field-name value."
  [form-name field-name]

  (let [ p (str (name form-name) "-" (name field-name) )
        ;;_ (debug "i18n-for p is " p)
        form-specific (j18n/resource (keyword p) my-bundle)
        standard (j18n/resource (keyword field-name) my-bundle)
        ;;_ (debug "i18n standard " standard " form specific " form-specific)
        ]
       ;;(debug "XYZALL " form-name " " field-name " ====> " form-specific  " STANDARD  " standard) 
       #_(if form-specific 
         (debug "FORM SPECIFIC " form-name " " field-name " ====> " form-specific ) 
         (if standard 
           (debug "standard " form-name " " field-name " ====> " standard ) 
           (debug "i18n-for returning p " form-name " " field-name " ====> " p))) 
       (if form-specific form-specific (if standard standard p)) ))

(defn i18n-placeholder-for
  [form-name field-name]
  (i18n-for form-name (str field-name "-placeholder")))

(defn i18n-label-for
  [form-name field-name]
  (i18n-for form-name (str field-name "-label")))
