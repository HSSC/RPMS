(ns org.healthsciencessc.rpms2.consent-collector.forms
  (:require [hiccup.core :as hiccup]
            [hiccup.page-helpers :as hpages]
            [hiccup.form-helpers :as hform])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

(defn generate-kw
    "Returns a keyword for a label item, to be used in 
    looking up a string in a resource bundle."
    [form-name field-name keyword-type]
    (keyword (str form-name "-" field-name "-" keyword-type )))

(defn label-kw
    "Returns a keyword for a label item, to be used in 
    looking up a string in a resource bundle."
    [form-name field-name]
    (generate-kw form-name field-name "label"))

(defn text-field2
   "Returns a text field in a div. The keywords for the label
   and name of the text field are used to lookup the 
   strings in the resource bundle."

;; Sample invocation (text-field2 "foo" "bar" :type "number" :required true)

   [form-name field-name & {:as input-opts}]

   (let [ placeholder-keyword (keyword (str form-name "-" field-name "-placeholder" ))]
   [:div  (i18n (label-kw form-name field-name)) 
    [:input (merge { :type "text" :name field-name :placeholder (i18n placeholder-keyword) :length 100 }
                   input-opts) ]]))

(defn submit-button 
    "Returns standard submit button for form."
    [form-name]

   (let [kw (keyword (str form-name "-submit-button" ))]
    [:input {:type "submit" :value (i18n kw)} ]))


(defn form-login 
     "The Login form"
     []
     [:form {:action "/view/login"
             :method "POST"} [:h1 (i18n :login-form-title) ]

      (i18n :login-form-username) 
      [:input {:id "username" :name "userid" :type "text" :required "" 
               :placeholder (i18n :login-form-username-placeholder) }]

      (i18n :login-form-password) 
      [:input {:name "password" :type "password" :required ""
               :placeholder (i18n :login-form-password-placeholder) }] 

      (submit-button  "login-form") ])

(defn form-select-location
  "Generates HTML page to select location from list of locations.
  Then redirects to view-select-lock-code"
  [locs]

  [:form {:method "POST" :action "/view/select/location" } 
	(i18n :select-location-form-location-label) 
   [:select {:name "location"} (hform/select-options locs)]
   [:div (submit-button "select-location-form") ]])

(defn form-select-lock-code 
  "Displays HTML form for entering the lockcode, which is a required
  4 digit number.  "

  []
  [:form {:method "POST" :action "/view/select/lock-code" }
   (i18n :lock-code-form-enter-lock-code ) 
   [:input {:name "lockcode" :type "number" :required "" :length 4 :min 0 :max "9999"}]
   (submit-button "lock-code-form") ])
