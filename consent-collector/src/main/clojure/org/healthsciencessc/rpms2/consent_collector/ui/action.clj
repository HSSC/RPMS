;; Provides helper functions for generating certain jquery scripts on the browser.
(ns org.healthsciencessc.rpms2.consent-collector.ui.action
  (:require [clojure.data.json :as json]
            [org.healthsciencessc.rpms2.consent-collector.text :as text])
  (:use     [pliant.process :only [defprocess]]))

(defn inline
  [options]
  (if (:inline options)
    "true"
    nil))

(defprocess form-submit
  "Creates an HTML rendering of a button which will submit the data on the form within which the button lives.  The data 
   is culled from the form inputs into a map which is submitted as the body.  The URL and method of the request are culled
   from the containing form."
  [{:keys [label] :as options}]
  [:button.action-form-submit
      {:data-ajax "false"
       :data-inline (inline options) 
       :data-role "button"
       :data-theme "a"  
       :type "button"} label])

(defprocess form-submit-state
  "Creates an HTML rendering of a button which will submit the data on the form within which the button lives.  The data 
   is culled from the 'state' data attribute on the form, completely ignoring any of the imputs.  The URL and method of 
   the request are culled from the containing form."
  [{:keys [label required] :as options}]
  [:button.action-form-submit-state
      {:data-ajax "false"
       :data-inline (inline options) 
       :data-role "button"
       :data-theme "a"
       :data-required (if required (json/json-str required))
       :type "button"} label])

(defprocess post-form-data
  "Creates an HTML rendering of a button which will make an ajax call to the provided URL.  The body of the request will 
   contain the data that is currently in the form within which the button lives.  Utilizes the method provided.  If method 
   is empty then the request is posted."
  [{:keys [label url method] :as options}]
  [:button.action-post-form-data
      {:data-inline (inline options) 
       :data-method method
       :data-role "button"
       :data-url url
       :data-theme "a" 
       :type "button"} label])

(defprocess post-form-state
  "Creates an HTML rendering of a button which will make an ajax call to the provided URL.  The body of the request will 
   contain the data that is stored as the 'state' data attribuite on form within which the button lives.  Utilizes the 
   method provided.  If method is empty then the request is posted."
  [{:keys [label url method] :as options}]
  [:button.action-post-form-state
      {:data-inline (inline options) 
       :data-method method
       :data-role "button"
       :data-url url
       :data-theme "a" 
       :type "button"} label])

(defprocess post-data
  "Creates an HTML rendering of a button which will make an ajax call to the provided URL.  
   The body of the request can be populated by both parameters provided to this method and/or 
   data that exists in a form which the button is contained in.  Utilizes the method provided. 
   If method is empty then the request is posted."
  [{:keys [label url method include-form parameters] :as options}]
  [:button.action-post-data
      {:data-inline (inline options) 
       :data-include-form (if include-form "true" "false")
       :data-parameters (if parameters (json/json-str parameters))
       :data-method method
       :data-role "button"
       :data-url url
       :data-theme "a" 
       :type "button"} label])

(defprocess next-view
  "Creates an HTML rendering of a button which will make an ajax call to the provided URL.  The body of the request will 
   contain the data that is currently in the form within which the button lives.  Utilizes the method provided.  If method 
   is empty then the request is posted."
  [{:keys [label url method include-form parameters] :as options}]
  [:button.action-next-view
      {:data-inline (inline options) 
       :data-include-form (if include-form "true" "false")
       :data-parameters (if parameters (json/json-str parameters))
       :data-method method
       :data-role "button"
       :data-url url
       :data-theme "a" 
       :type "button"} label])

(defprocess back
  "Creates an HTML rendering of a button which cause the page manager to navigate back in history."
  [options]
  [:a.action-back
      {:data-inline (inline options) 
       :data-rel "back"
       :data-role "button"
       :data-theme "a"} (or (:label options) (text/text :action.back.label))])

(defprocess cancel
  "Creates an HTML rendering of a button which will cancel the consent collection session for the current consenter."
  [options]
  [:button.action-cancel
      {:data-ajax "false"
       :data-inline (inline options) 
       :data-role "button"
       :data-theme "a"
       :data-title (text/text :cancel.consent.title)
       :data-message (text/text :cancel.consent.message)
       :type "button"} (or (:label options) (text/text :action.cancel.label))])

(defprocess logout
  "Creates an HTML rendering of a button which log the current user out of the system.."
  [options]
  [:button.action-logout
      {:data-ajax "false"
       :data-inline (inline options) 
       :data-role "button"
       :data-theme "a"  
       :type "button"} (text/text :action.logout.label)])