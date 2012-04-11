(ns org.healthsciencessc.rpms2.consent-collector.helpers
  "General purpose helpers used when creating views."
  (:require [hiccup.core :as hiccup]
            ;[clojure.string :as s]
            ;[org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
            [hiccup.page-helpers :as hpages]
            ;[hiccup.form-helpers :as hform]
            )
  (:require [ring.util.response :as ring])
  (:use [sandbar.stateful-session :only [session-get session-put! destroy-session! flash-get]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [clojure.string :only (replace-first join)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only (i18n i18n-existing)]))

;; web application context 
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
  "Returns context where consent-collector is displayed when needed."
  [url]
  (absolute-path (remove-initial-slash url)))

(defn myredirect
  "Redirect, adding context information as needed."
  [url]
  (ring/redirect (mypath url)))

(defn username
  []
  (-> (session-get :user) :username))

(defn generate-kw
  "Returns a keyword for a label item, to be used in 
  looking up a string in a resource bundle."
  [form-name field-name keyword-type]
  (keyword (str form-name "-" field-name "-" keyword-type)))

(defn placeholder-kw
  "Returns a keyword for a label item, to be used in 
  looking up a string in a resource bundle."
  [form-name field-name]
  (generate-kw form-name field-name "placeholder"))

(defn name-value-cb
  "Creates a div with a label and name, which will be horizontally styled.
   Setting data-role to 'fieldcontain' tells jquery mobile to group the 
   label and the value and display horizontally if possible."
  [v id]
  [:div {:data-role "fieldcontain" } 
    ;; [:label {:for v :class "labelclass" } v ]
    [:input {:type "checkbox" :name v :value v } ]])

(defn name-value
  "Creates a div with a label and name, which will be horizontally styled.
   Setting data-role to 'fieldcontain' tells jquery mobile to group the
   label and the value and display horizontally if possible."
  [form-name v id]
  [:div {:data-role "fieldcontain" } 
   [:label {:for v :class "labelclass" } (i18n form-name v "label" ) ]
   [:div.value { :id id } v ]])

(defn name-value-bold
  "Creates a div with a label and name, which will be horizontally styled.
   Setting data-role to 'fieldcontain' tells jquery mobile to group the 
   label and the value and display horizontally if possible."
  [form-name v id]
  [:div.valueimportantblock {:data-role "fieldcontain" } 
    [:label {:for v :class "labeldim" } (i18n form-name v "label") ]
    [:div.highlightvalue { :id id } ]])


(defn name-value-bold-input
   "Creates a div with a label and name, which will be horizontally styled.
    Setting data-role to 'fieldcontain' tells jquery mobile to group the 
    label and the value and display horizontally if possible."
   [form-name v id]

   [:div.valueimportantblock {:data-role "fieldcontain" } 
     [:label {:for v :class "labeldim" } (i18n form-name v "label") ]
     [:div.highlightvalue { :id id } ]])


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

   (let [ 
	placeholder-keyword (keyword (str form-name "-" field-name "-placeholder" ))
	type-keyword (keyword (str form-name "-" field-name "-type" ))
	type-value (i18n-existing type-keyword)
	type  (if type-value type-value "text")
	]
   [:div  {:data-role "fieldcontain" } 
      [:label {:for field-name :class "labelclass" } (i18n form-name field-name "label") ]
      [:input (merge { :type type :name field-name :placeholder (i18n placeholder-keyword) :length 100 } input-opts) ]]))


(defn submit-button
  "Returns standard submit button for form."
  [form-name]

  (let [kw (keyword (str form-name "-submit-button" ))]
    [:input {:type "submit" :value (i18n kw) :name (str form-name "-submit-button") } ]))

(defn standard-form 
  "Wraps form in a standard structure."
  [method action & body]

  [:div.standardForm [:form {:method method :action action } body ] ])

(def progress-indicator
   "Using the current context, determines which
   progress dots should be displayed and displays them."
  [:span.progressArea "" ])

(defn- header
 [title]
  [:div.header 
  [:div.ui-grid-b
   [:div.ui-block-a [:div 
                      [:div.title title]
              #_(progress-indicator)  ; this is no longer function
     (if-let [msg (flash-get :header)]
       [:div#flash msg])] ]
   [:div.ui-block-b [:div " RPMS2 "] ]
   [:div.ui-block-c [:div (if-let [name (username)]
			(hpages/link-to "/logout" "logout" ))]]]])


(defn- patient-description
  []
  (let [[n id d] (map session-get [:patient-name :patient-id :patient-encounter-date])]
    (if n
      (format "Patient Name: %s Encounter ID: %s Date: %s" n id d)
      "RPMS2")))

(defn- patient-footer [] [:div.footer (patient-description) ])

(defn- non-patient-footer
  []
  [:div.footer 
  [:div.ui-grid-b
   [:div.ui-block-a 
	(if-let [nm (username)] 
		[:div (hpages/link-to "/logout" (str "Logout " nm)) (hpages/link-to "/login" "login" ) ]) ] 
   [:div.ui-block-b "RPMS2 (no patient)" ]
   [:div.ui-block-c (if-let [nm (username)]
                      [:div#header-userid nm
                       (if-let [loc (session-get :location)] (str " @ " loc)) ] )]]])


(defn- footer
  []
  (if (= (session-get :patient-id) "no patient") (non-patient-footer) (patient-footer) ))

(defn remove-session-data
  "Remove session data"
  []
  (destroy-session!))


(defn- head-part-jquery
  []
  [:head
    "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, user-scalable=0\" >"
    (hpages/include-css 
     (absolute-path "app.css")
     "http://code.jquery.com/mobile/1.0.1/jquery.mobile-1.0.1.min.css" )
    (hpages/javascript-tag "var CLOSURE_NO_DEPS = true;")
    (hpages/include-js 
     "http://code.jquery.com/jquery-1.6.4.min.js"
     "http://code.jquery.com/mobile/1.0.1/jquery.mobile-1.0.1.min.js"
     (absolute-path "app.js"))])

(def ipad-html5-class
  "ui-mobile landscape min-width-320px min-width-480px min-width-768px min-width-1024px" )

(defn rpms2-page
  "Emits a standard RPMS2 page."
  [content & {:keys [title]}]

  (hpages/html5 {:class ipad-html5-class }
    [:head
    "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, user-scalable=0\" >"
    (hpages/include-css 
     (absolute-path "app.css")
     "http://code.jquery.com/mobile/1.0.1/jquery.mobile-1.0.1.min.css" )
    (hpages/javascript-tag "var CLOSURE_NO_DEPS = true;")
    (hpages/include-js 
     "http://code.jquery.com/jquery-1.6.4.min.js"
     "http://code.jquery.com/mobile/1.0.1/jquery.mobile-1.0.1.min.js"
     (absolute-path "app.js"))]
   [:body 
    [:div {:data-role "page" } 
    [:div#header {:data-role "header" } 
      (try (header title) (catch Exception ex (str "UNABLE TO GENERATE HEADER " ex )))
    ]
    [:div#content {:data-role "content" } content]
    [:div#footer {:data-role "footer" } (footer)]]
    ]))

(defn rpms2-page-two-column
  "Emits a standard two-column RPMS2 page."
  [col1-content col2-content & {:keys [title]}]

  (rpms2-page 
	[:div.ui-grid-f
	     [:div#search-consenter-list.ui-block-a (doall col1-content) ]
	     [:div.ui-block-b (doall col2-content) ]]
        :title title ))
