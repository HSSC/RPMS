(ns org.healthsciencessc.rpms2.consent-collector.helpers
  "General purpose helpers used when creating views."
  (:require [hiccup
             [page :as hpage]
             [element :as helem]])
  (:require [ring.util.response :as ring])
  (:use [sandbar.stateful-session :only [session-get session-put! session-delete-key! destroy-session! flash-get flash-put!]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [clojure.string :only (replace-first join)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only (i18n i18n-existing)]))

;; web application context, bound in core
(def ^:dynamic *context* "")

(defn absolute-path
  [& path-elements]
  (str *context* "/" (join "/" path-elements)))

(defn logged-in?
  []
  (if (session-get :user) true false))

(defn- remove-initial-slash
  [s]
  (if (re-matches #"\/.*" s)
    (subs s 1)
    s))

(defn mypath 
  "Converts the url to an absolute path, taking into account the context."
  [url]
  (absolute-path (remove-initial-slash url)))

(defn myredirect
  "Redirect, adding context information as needed."
  [url]
  (ring/redirect (mypath url)))

(defn flash-and-redirect
  "Sets flash message and goes to the specified page."
  [i18n-key path]

  (debug "flash-and-redirect  " (i18n i18n-key) " path " path)
  (flash-put! :header (i18n i18n-key))
  (myredirect path))

(defn username
  []
  (let [u (session-get :user)]
        (if u (:username u) nil)))

(defn text-field3
   "Returns a text field in a div. The keywords for the label
   and name of the text field are used to lookup the 
   strings in the resource bundle.
   
   Lookup the type and use that if it's available.  If no type
   is specified, type defaults to text.

   Example data in resource file:
   my-form-variable=loc1
   my-form-variable-label=Location 1
   my-form-variable-placeholder=Enter Location 1

   my-form-zipcode=zipcode
   my-form-zipcode-label=Zipcode
   my-form-zipcode-placeholder=Enter Zipcode
   my-form-zipcode-type=number
   "

;; Sample invocation (text-field3 "foo" "bar" :type "number" :required true)

   [form-name field-name & {:as input-opts}]

   (let [ placeholder-keyword (keyword (str form-name "-" field-name "-placeholder" ))
	type-keyword (keyword (str form-name "-" field-name "-type" ))
	type-value (i18n type-keyword)
	;type-value (i18n-existing type-keyword)
	t  (if type-value type-value "text") ]
   [:div.inputdata  {:data-role "fieldcontain" } 
      [:label {:for field-name :class "labelclass" } (i18n form-name field-name "label") ]
      [:input (merge { :type t :name field-name :placeholder (i18n placeholder-keyword) :length 100 } input-opts) ]]))



(defn submit-button
  "Returns submit button for form."
  ([form-name]
    (submit-button (i18n form-name "submit-button")
                   (str "-submit-button"))) 
  ([v n] 
    [:input 
      {:type "submit" 
       :data-theme "a"
       :data-role "button"
       :data-inline "true"
       :value v :name n } ])


  ([form-name v n] 
    [:input 
      {:type "submit" 
       :data-theme "a"
       :data-role "button"
       :data-inline "true"
       :value v :name n } ])
  )

(defn ajax-submit-button
  [form-name]
  (submit-button form-name))

(defn standard-form 
  "Wraps form in a standard structure."
  [method action & body]

  [:div.innerform [:form {:method method :action action :data-ajax "false" } 
    body ] ])


(defn post-form 
  [path body & submit-buttons ]

  [:div
     [:form {:action (mypath path) 
             :method "POST" 
             :data-ajax "false" 
             :data-theme "a" } 
      [:div.innerform.centered body ] 
      [:div.centered submit-buttons ] ]]) 


(defn remove-session-data
  "Remove session data"
  []
  ;(destroy-session!)
  (doseq [k [ :patient-id 
            :patient-name 
            :patient-encounter-date 
            :location 
            :org-location 
            :org-name
            :lockcode
            :user  ]]
            (session-delete-key! k)))

(def ipad-html5-class
  "ui-mobile landscape min-width-320px min-width-480px min-width-768px min-width-1024px" )

(defn rpms2-page
  "Emits a standard RPMS2 page."
  [content & {:keys [title]}]

  (hpage/html5 {:class ipad-html5-class }
    [:head
    "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0\" >"
    "<meta name=\"apple-mobile-web-app-capable\" contents\"yes\" />"
    (hpage/include-css 
     (absolute-path "app.css")
     "http://code.jquery.com/mobile/1.1.0/jquery.mobile-1.1.0.min.css" )

    (helem/javascript-tag "var CLOSURE_NO_DEPS = true;")
    (helem/javascript-tag (format "var RPMS2_CONTEXT = %s;" (pr-str *context*)))
    (hpage/include-js 
     "http://code.jquery.com/jquery-1.7.1.min.js"
     "http://code.jquery.com/mobile/1.1.0/jquery.mobile-1.1.0.min.js"
     (absolute-path "app.js"))
     ; ${function() { ${ \"button, input:submit, input:button\" ).button(); });
     ]
   [:body 
    [:div {:data-role "page" :data-theme "a"  }  
      [:div {:data-role "header" } (if-let [msg (flash-get :header)] 
                                     [:div title [:div#flash msg ]]
                                     [:h1 title ] ) ]
      [:div#content {:data-role "content" :data-theme "d" } content]]]))

(defn rpms2-page-two-column
  "Emits a standard two-column RPMS2 page."
  [col1-content col2-content title]

  (rpms2-page 
	[:div.ui-grid-f
	     [:div.ui-block-a col1-content ]
	     [:div.ui-block-b col2-content ]]
        :title title ))

