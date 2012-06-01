(ns org.healthsciencessc.rpms2.consent-collector.helpers
  "General purpose helpers used when creating views."
  (:require [hiccup
               [page :as hpage]
               [element :as helem]])
  (:require [ring.util.response :as ring])
  (:use [sandbar.stateful-session :only [session-get session-put! session-delete-key! destroy-session! flash-get flash-put!]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [clojure.string :only (replace-first join)])
  (:use [clojure.data.json :only (read-json json-str)])
  (:use [clojure.pprint])

  (:use [org.healthsciencessc.rpms2.consent-collector.debug :only [debug! pprint-str]])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only (i18n i18n-existing
                                                                       i18n-label-for
                                                                       i18n-placeholder-for)]))

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
  "Extracts the username from the currently logged in user."
  []
  (let [u (session-get :user)]
        (if u (:username u) nil)))

(defn authorized-locations
  []
  (->> (session-get :user)
        :role-mappings
        (filter (comp #{"Consent Collector"} :name :role))
             (map :location)))
       
(defn current-org-id
  "Extracts the org-id from the currently logged in user.
  Maybe we should use the org associated with the user record
  instead of the location"
  []
  (get-in (session-get :org-location) [:organization :id]))
  ;;(get-in (session-get :user) [:organization :id]))

(defn custom-site-label
  "Returns the location label, which is taken from the user's location
  if available; otherwise this is taken from the organization.
  If neither the organization or location has a value, then uses a default val"
  [label-nm defaultval]
  (let [u (session-get :user)
        l (session-get :org-location)
        loc-specific (get-in l [:location label-nm ])
        org-specific (get-in u [:organization label-nm ])
        retval (if (empty? loc-specific) 
                   (if (empty? org-specific) 
                       defaultval 
                       org-specific)
                   loc-specific) ]
      (debug "custom-site-label |", label-nm "|" defaultval, "|u|" u, "|l|" l, " loc spec ", loc-specific " org-specific ", org-specific " RETURNING " retval)
    retval))

(defn org-location-label
  "Returns the location label, which is taken from the user's location
  if available; otherwise this is taken from the organization."
  []
  (custom-site-label :location-label "Location"))

(defn org-protocol-label
  "Returns the protocol label, which is taken from the user's location
  if available; otherwise this is taken from the organization."
  []
  (custom-site-label :protocol-label "Protocol" ))

(defn standard-submit-button 
  "Standard submit button.  :name and :value should be define plus optionally other."
  [input-opts]
  [:input (merge { :type "submit" 
                   :data-theme "a"
                   :data-role "button"
                   :data-ajax "false" 
                   :data-inline "true"
                   } input-opts) ])

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

(defn post-form 
  [path body & submit-buttons ]

  [:div.centered.post-form
     [:form {:action (mypath path) 
             :method "POST" 
             :data-ajax "false" 
             :data-theme "a" } 
      [:div.innerform.centered body ] 
      [:div.submit-area submit-buttons ] ]] ) 


(defn collect-consent-form 
  "the style is the only difference."
  [path body & submit-buttons ]

  (let [retval [:div.collect-consent-form
     [:form {:action (mypath path) 
             :method "POST" 
             :data-ajax "false" 
             :data-theme "a" } 
      [:div body ] 
      [:div.submit-area submit-buttons ] ]]] 
      ;;(debug "collect-consent-form " (pprint retval)) 
     retval)) 


(defn remove-session-data
  "Remove session data"
  []
  ;(destroy-session!)
  (debug "remove-session-data")
  (doseq [k [ :patient-id 
            :patient-name 
            :patient-encounter-date 
            :location 
            :org-location 
            :org-name
            :lockcode
            :search-params
            :create-params
            :user  ]]
            (session-delete-key! k)))

(def ipad-html5-class
  "ui-mobile landscape min-width-320px min-width-480px min-width-768px min-width-1024px" )

(defn- logout-form
  []
  [:form {:method "POST" :action (absolute-path "/view/logout") }
           [:input {:type "submit" 
                    :name "logout-btn" 
                    :data-role "button"
                    :data-inline "true"
                    :data-theme "a"
                    :data-mini "true"
                    :data-ajax "false" 
                    :value "Logout" } ]])

(defn cancel-form
  [path]
  [:form {:method "GET" :action (absolute-path path)  }
           [:input {:type "submit" 
                    :name "cancel-btn" 
                    :data-role "button"
                    :data-inline "true"
                    :data-theme "a"
                    :data-mini "true"
                    :data-ajax "false" 
                    :value "Cancel" } ]])

(defn- header-collect-consents
  [title]
  [:div.header {:data-role "header" } 
     [:div#consent-header
     [:div.ui-grid-i
         [:div.ui-block-aa (cancel-form "/view/select/consenter") ]
         [:div.ui-block-bb.title title ]
         [:div.ui-block-cc (if-let [u (session-get :user)] (logout-form))]]] 
     [:div (if-let [msg (flash-get :header)] [:div#flash msg ]) ] ]) 

(defn- header-standard
  [title & cancel-btn]
  [:div.header {:data-role "header" } 
     [:div.ui-grid-b 
        [:div.ui-block-a cancel-btn ]
        [:div.ui-block-b.title title ]
        [:div.ui-block-c (if-let [u (session-get :user)] (logout-form))]] 
     [:div (if-let [msg (flash-get :header)] [:div#flash msg ]) ] ] )

(defn- header
  [title cancel-btn]
  (if (session-get :collect-consent-status) 
      (header-collect-consents title)
      (header-standard title cancel-btn)))

(defn- footer
  []
  [:div.footer {:data-role "footer" :data-theme "c" } 
    [:div.ui-grid-b
      [:div.ui-block-a (if-let [loc (session-get :location)]
                               (str (org-location-label) ": " loc)) ]

      [:div.ui-block-b (if-let [p (session-get :patient-name)]
                               (str "Consenter: " p)) ]

      [:div.ui-block-c (if-let [p (session-get :encounter-id)]
                               (str "EncounterID: " p)) ] ]])


(defn rpms2-page
  "Emits a standard RPMS2 page."
  [content & {:keys [title cancel-btn]}]

  (let [resp (hpage/html5 {:class ipad-html5-class }
    [:head
    "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0\" >"
    "<meta name=\"apple-mobile-web-app-capable\" contents=\"yes\" />"
    (hpage/include-css 
     (absolute-path "app.css")
     (absolute-path "jquery.mobile-1.1.0.min.css" )
     (absolute-path "jquery.signaturepad.css" )
     )

    (helem/javascript-tag "var CLOSURE_NO_DEPS = true;")
    (helem/javascript-tag (format "var RPMS2_CONTEXT = %s;" (pr-str *context*)))
    (hpage/include-js 
     (absolute-path "jquery-1.7.1.min.js")
     (absolute-path "jquery.mobile-1.1.0.min.js")
      ;; see http://thomasjbradley.ca/lab/signature-pad/
     (absolute-path "flashcanvas.js")
     (absolute-path "jquery.signaturepad.js")
     (absolute-path "json2.js")
     (absolute-path "app.js")
      ) ]
   [:body 
    [:div {:data-role "page" :data-theme "a"  }  
      (header title cancel-btn)
      [:div#content {:data-role "content" :data-theme "d" } content]
      (footer) ]])] 
      (debug "Page: " title " is\n" (pprint-str resp) "\n")
      resp))

(defn rpms2-page-two-column
  "Emits a standard two-column RPMS2 page."
  [col1-content col2-content title cancel-btn]

  (rpms2-page 
	[:div.ui-grid-f
	     [:div.ui-block-a col1-content ]
	     [:div.ui-block-b col2-content ]]
        :title title 
        :cancel-btn cancel-btn ))


(defn radio-btn
  "Returns a radio button."

  ([group-name btn-name] 
     (radio-btn group-name btn-name btn-name))
  ([group-name btn-name btn-id]
     (radio-btn group-name btn-name btn-name {}))
  ([group-name btn-name btn-id m]

  (list [:input (merge {:type "radio" 
                 :name group-name
                 :id btn-id 
                 :value btn-name } m )]
        [:label {:for btn-name } btn-name ] )))

(defn radio-btn-group
  "Handles jquerymobile radio button group.
  {:btnlist collection :group-name g :selected-btn b}"
  [m]

  [:fieldset {:data-role "controlgroup" }
   (list (map 
           (fn [btn-name] 
             (radio-btn 
                (:group-name m) 
                btn-name 
                btn-name
                (if (= (:selected-btn m) btn-name) {:checked "checked" } {} ))) 
             (:btnlist m) )) ])

(defn checkbox-group
  "{:name n :value v}"
  [m]
  (debug "checkbox-group " m )
  [:div {:data-role "fieldcontain" }
   [:input (merge 
             {:type "checkbox" :id (:name m) :name (:name m) } 
             (if (= (:value m) "on") {:checked "checked" } {} )) ]
   [:label {:for (:name m) } (:label m) ]])

(defn gender-control
  "Creates drop down for selecting gender. "
  [field]

  [:div.inputdata {:data-role "fieldcontain" } 
     [:label {:for field :class "labelclass" } "Gender" ]
     [:select {:name field :id field }
        [:option {:value "F" } "Female" ]
        [:option {:value "M" } "Male" ]
     ]])

(defn emit-field
  "Emits a field definition. 
  Two variations - with or without map containing default values."

  ([field-def form-name field ]  
    (emit-field field-def form-name field {}))

  ([field-def form-name field default-values]

  (list (let [specified-kind (:type field-def)
              t (if (and specified-kind (not (= specified-kind "gender"))) 
                  specified-kind 
                  "text")
              normalized-field (if-let [n (:i18n-name field-def)] n field)

       mx {:type t 
           :class "inputclass" 
           :id field
           :name field
           :placeholder (i18n-placeholder-for form-name normalized-field)
          }

       ;; should also verify that is a valid value
       m  (merge mx 
            ;;(if (= "readonly" augment) {:readonly ""} {})
            (if (= t "date") {:data-theme "d" 
                              :data-role "datebox" 
                             } {})
            (if (:required field-def) {:required ""} {})
            (if-let [val-fn (:default-value field-def) ] 
              {:value (val-fn)} 
              (if-let [v (get default-values (keyword field))] {:value v } {})))
        ]
          (if (= specified-kind "gender")
             (gender-control field)

       [:div.inputdata {:data-role "fieldcontain" } 
            [:label {:for field
                     :class "labelclass" } 
                     (i18n-label-for form-name normalized-field) ]
            [:input m ]])))))



(defn n-get-widgets [slist] 
  (doall (for [wlist slist] (:contains wlist) )))

(defn all-widgets-in-form
  "Traverses the pages and sections to get the widget names"
  [a]
  (flatten 
    (do
      (for [s (map (fn [n] (get-in n [:contains])) (get-in a [:form :contains]))]
             (flatten (n-get-widgets s))
        ))))

(defn all-widget-props-in-form
  "Traverses the pages and sections to get the widget names"
  [a prop]
  (flatten 
    (do
      (for [s (map (fn [n] (get-in n [:contains])) (get-in a [:form :contains]))]
        (map prop (flatten (n-get-widgets s)))))))

(defn all-widget-names-in-form
  "Returns a list of all widget names in this form."
  [form]
  (all-widget-props-in-form form :name))

(defn all-widget-types-in-form
  "Returns a list of all widget types in this form."
  [form]
  (all-widget-props-in-form form :type ))


(defn lookup-widget-by-name
  "Lookup the widget by name."
  ([n]
   (let [d (session-get :current-form-data)]
     (if d (lookup-widget-by-name d n))))
    
  ([all n]
    (first (filter #(= (:name %) n) all))))

;;(println "Medical Treatment Text " (lookup-widget-by-name data-model "MedicalTreatmentText"))
;;(println "Privacy Practices " (lookup-widget-by-name data-model "PrivacyPracticesText"))
;;(println "bad " (lookup-widget-by-name data-model "badname"))

(defn clear-consents
  "Remove any in-progress consent information."
  []
  (debug "clearing consent data")
  (session-delete-key! :collect-consent-status)
  (session-delete-key! :model-data))

(defn create-form-data
  [n]
  {:hi "Tami" })

(defn print-form
  [orig-n]
  (let [n (if (seq? orig-n) (first orig-n) orig-n)]
     (get-in n [:protocol :name])))

(defn init-consents
  "Initializes the consent collection data structures."
  [f]
  (debug "initializing consent data : form is " f)
  (session-put! :current-form f)
  (let [formlist (session-get :protocols-to-be-filled-out)
        wnames (all-widget-names-in-form f) ]
    (try
      (do
        (debug "WNAMES IS " wnames)
        (session-put! :current-form-data (all-widgets-in-form f))
        (session-put! :current-form-names wnames )
        (debug "form list count is " (count formlist))
        (debug "LIST MAP = " (pprint-str (list (map print-form formlist)) ))
        (list (map print-form formlist)) 
        formlist)
    (catch Exception e (do 
                         (debug "init-consents EXCEPTION Ex " e)
                         (println "init-consents EXCEPTION Ex " e)
                         (.printStackTrace e))))))

(defn init-review
  []
  (debug "init-review")
)

(defn clear-patient
  []

  (debug "clear patient: ")
  (session-delete-key! :patient-id)
  (session-delete-key! :patient-name)
  (session-delete-key! :encounter-id) 
  (session-delete-key! :patient-encounter-date))

(defn set-patient
  "Saves patient info in the session."
  [m]

  (debug "set patient: " m)
  (session-put! :patient-id (:patient-id m))
  (session-put! :patient-name (:patient-name m))
  (session-put! :encounter-id (:encounter-id m))
  (session-put! :patient-encounter-date (:patient-encounter-date m)))
