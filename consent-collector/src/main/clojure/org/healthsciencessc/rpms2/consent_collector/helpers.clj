(ns org.healthsciencessc.rpms2.consent-collector.helpers
  "General purpose helpers used when creating views."
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

(def ^:const COLLECT_START_PAGE :collect-start)
(def ^:const REVIEW_START_PAGE :summary-start)

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
  "Redirect to url, adding context information as needed.
  If a message is specified and debugging is enabled, 
  the message is displayed as a flash message"
  ([i18n-key url]
   (if-let [b (config "verbose-collect-consents")]
     (do
      (debug "myredirect  " (i18n i18n-key) " url " url)
      (flash-put! :header (i18n i18n-key))
      (myredirect url))
     (myredirect url)))

  ([url]
  (debug "myredirect url " url)
  (ring/redirect (mypath url))))

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
       
(defn- pg-dbg 
  [s]
  (if-let [b (config "verbose-page")]
    (debug s)))

(defn current-org-id
  "The org-id from the currently logged in user."
  []
  (get-in (session-get :user) [:organization :id]))

(defn org-location-label
  "Returns location label, which is taken from the user's location
  if available; otherwise this is taken from the organization."
  []
  (label-for-location (session-get :org-location) (:organization (session-get :user))))

(defn org-protocol-label
  "Returns protocol label, which is taken from the user's location
  if available; otherwise this is taken from the organization."
  []
  (label-for-protocol (session-get :org-location) (:organization (session-get :user))))

(defn submit-btn 
  "Standard submit button.  :name and :value should be define plus optionally other."
  ([] (submit-btn {}))

  ([input-opts]
   [:input (merge {:type "submit" 
                   :data-theme "a"
                   :data-role "button"
                   :data-ajax "false" 
                   :data-inline "true"
                  } input-opts) ] ))


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

(defrecord FormStatus [forms cform state])

(defn make-form-status [forms cform state]
  {:pre [(>= (count forms) 0)] }   ; ensure there's at least one form
  (FormStatus. forms cform state))

(defn- header
  [title cancel-btn]
  (if (session-get :collect-consent-status) 
      (header-collect-consents title)
      (header-standard title cancel-btn)))

(defn- after-content
  "Enable signature pad."
  []
  (if (session-get :collect-consent-status) 
      [:script "// var api = $('.sigPad').signaturePad();
               $(document).ready( function() { $('.sigPad').signaturePad({drawOnly: true}); });" 
      ]))  

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
  [content & {:keys [title cancel-btn end-of-page-stuff]}]

  (let [resp (hpage/html5 {:class ipad-html5-class }
    [:head
    "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0\" >"
    "<meta name=\"apple-mobile-web-app-capable\" contents=\"yes\" />"
    (hpage/include-css 
     (absolute-path "app.css")
     (absolute-path "jquery.mobile-1.1.0.min.css" )
     (absolute-path "jquery.signaturepad.css" ))

    (helem/javascript-tag "var CLOSURE_NO_DEPS = true;")
    (helem/javascript-tag (format "var RPMS2_CONTEXT = %s;" (pr-str *context*)))
    (hpage/include-js 
     (absolute-path "jquery-1.7.1.min.js")
     (absolute-path "jquery.mobile-1.1.0.min.js")
      ;; see http://thomasjbradley.ca/lab/signature-pad/
     (absolute-path "jquery.signaturepad.js")
     (absolute-path "json2.js")
     (absolute-path "flashcanvas.js")
     (absolute-path "app.js")) ]
   [:body 
    [:div {:data-role "page" :data-theme "a"  }  
      (header title cancel-btn)
      [:div#content {:data-role "content" :data-theme "d" } 
      content
      (after-content)
       "<!-- ZZZ BEGIN END OF PAGE -->"
      end-of-page-stuff 
       "<!-- ZZZ AFTER END OF PAGE -->"
       ]
      (footer) 
     ]])] 
      (pg-dbg (str "Page: " title " is\n" (pprint-str resp) "\n"))
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
     (radio-btn group-name btn-name btn-id {}))
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
  "Returns checkbox group, with jquerymobile attributes."
  [m]
  [:div {:data-role "fieldcontain" }
   [:input (merge {:type "checkbox" 
                   :id (:name m) 
                   :name (:name m) } 
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

(defn get-named-page
  "Find page named 'n' in form 'f'"
  [f n]
  (first (filter #(= (:name %) n ) (:contains f) )))

(defn data-for
  [c]
  (get (session-get :model-data) (keyword (:name c))))

(defn clear-return-page
  []
  (session-delete-key! :review-consent-page-in-progress))

(defn get-return-page
  []
  (session-get :review-consent-page-in-progress))

(defn save-return-page
  []
  (let [s (session-get :collect-consent-status) 
          cur-page (:page-name s)]
        (do
          (debug "save-return-page: " cur-page)
          (session-put! :review-consent-page-in-progress cur-page))))

(defn save-captured-data
  "Updates model with the information from the map.
  m contains the form post parameters. "
  [m]
  (let [orig (session-get :model-data) 
        m1 (dissoc (merge orig m) :next :previous) ]
        (session-put! :model-data  m1)))

(defn clear-consents
  "Remove any in-progress consent information."
  []
  (debug "clearing consent data")
  (session-delete-key! :collect-consent-status)
  (session-delete-key! :model-data))

(defn print-form
  "If orig-n is a sequence, use the first item.
  Otherwise, use orig-n, then return the protocol name."
  [orig-n]
  (let [n (if (seq? orig-n) (first orig-n) orig-n)]
     (get-in n [:protocol :name])))

(defn get-nth-form
  "Uses hardcoded mock data. Should return the nth 
  form (from :protocols-to-be-filled-out)."
  [n]
  (cond 
    (>= n (count (session-get :protocols-to-be-filled-out)))
    nil

    (= 0 n) 
    mock/lewis-blackman-form 

    (= 1 n) 
    mock/sample-form 
    
    :else
    mock/lewis-blackman-form))

(defn update-session
  "Merges the map, logs the new map, saves in session, and returns merged map."
  ([m] (update-session m ""))
  ([m msg]
   (let [new-map (merge (session-get :collect-consent-status) m)]
       (debug "update-session: " msg " " new-map)
       (session-put! :collect-consent-status new-map)
       new-map)))

(defn- get-form-name-kw
  "Each form is named by form-n where n is the number."
  [n]
  (keyword (str "form-" n)) )

(defn pr-truncate 
  "there's probably an output settig for this"
  [n]
  (let [s (pprint-str n)
        len (count s)]
    (if (> len 40) 
        (str (.substring s 0 40) "...")
        s)) )

(defn pr-form
  "Prints the form without the :output value.
  Want to replace keys starting with out"
  [ff]
  (if (map? ff)
      (let [okeys (filter #(.startsWith (name %) "output") (keys ff))]
        (if (> (count okeys) 0) 
            (str "okeys: " (list okeys) " " (pprint-str (dissoc ff (flatten okeys))))
            (pprint-str ff)))
      (str "not map " (pprint-str ff))))

(defn pr-model-data
  []
  (pr-form (session-get :model-data)))

(defn print-all-form-data
 []
 (let [ff (session-get :finished-forms)]
   [:div [:ol (for [f (keys ff) ]
         [:li "Form " [:span.standout f ] (pprint-str (pr-form (f ff) ) )] )]]))

(defn current-form
  "Returns the current form which is being processed."
  []
  (let [s (session-get :collect-consent-status)]
        (:form s))) 

(defn- get-data-for-nth-finished-form
  "If there is data saved for the nth form, return it.
  Otherwise return an empty map." 
  [n]
  (let [forms-data (if-let [f (session-get :finished-forms)] f {}) ]
        (get forms-data (get-form-name-kw n))))

(defn finish-form
  "Save data of the current form and prepare for the next one."
  []
  (let [s (session-get :collect-consent-status)
        n (:current-form-number s)
        finform  (if-let [finished-forms (session-get :finished-forms)] finished-forms {})
        k  (get-form-name-kw n)
        ff (assoc finform k (session-get :model-data))  ;; add current data to finished-forms
        ] 
    (do
      (session-put! :finished-forms ff)
      ;; if we are in review then load up saved data for the form
      (if (= (:which-flow s) COLLECT_START_PAGE ) 
          (session-put! :model-data {} )
          (session-put! :model-data (get-data-for-nth-finished-form (inc n))))

      (if-let [next-form (get-nth-form (inc n))]
       (let [formval (:form next-form)
             start-page-nm ((:which-flow s) formval)
             p (get-named-page formval start-page-nm) ;; get first page form, using specified flow
             modified-state {:form formval
                             :page p
                             :page-name (:name p)
                             :current-form-number (inc n) 
                            }]
            (update-session modified-state "finish-form" ))
       nil ))))
  

(defn- init-flow 
  "Initializes consent collection data structures.
  Sets :form to the the current form, initializes :page "
  [which-flow]
  (debug "init-flow: " which-flow)
  (let [form (get-nth-form 0)
        fform  (:form form)
        pg-nm (which-flow fform)
        m {:form fform 
           :page (get-named-page fform pg-nm)
           :page-name pg-nm
           :current-form-number 0
           :which-flow which-flow
          }]
    (do
      (if-not (:page m) 
              (do
                (flash-put! :header "MISSING PAGE " pg-nm)
                (error "init-flow MISSING PAGE: " pg-nm )))
      (update-session m "init-flow") )))

(defn init-review
  "Initializes consent collection data structures
  for review."
  []
  (init-flow REVIEW_START_PAGE)
  (session-put! :model-data (get-data-for-nth-finished-form 0)))

(defn set-page
  [page]
  (debug "Setting page: " (:name page) " " page)
  (update-session {:page page :page-name (:name page) }))

(defn init-consents
  "Initializes the consent collection data structures
  for collection."
  []
  (init-flow COLLECT_START_PAGE ))

(defn dbg-session
  [msg]
  (debug msg " user: " (pprint-str (session-get :user)))
  (debug msg " location: " (pprint-str (session-get :location)))
  (debug msg " org-location: " (pprint-str (session-get :org-location))))

(defn clear-location
  "Removes location information from session."
  []
  (session-delete-key! :location)
  (session-delete-key! :org-location))

(defn clear-patient
  "Removes patient information from session."
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

