(ns org.healthsciencessc.rpms2.consent-collector.core
  (:require [org.healthsciencessc.rpms2.process-engine [core :as pe]
                                                       [web-service :as ws]]
            [hiccup.core :as hiccup]
            [hiccup.page-helpers :as hpages]
            [hiccup.form-helpers :as hform]
            [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
            [ring.middleware.resource])
  (:use [sandbar.stateful-session :only [session-put!
                                         session-get
                                         session-delete-key!
                                         flash-put!
                                         flash-get]])
  (:use [ring.util.response :only [redirect]])
  (:use [org.healthsciencessc.rpms2.consent-collector.forms])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use clojure.pprint)
  (:import org.healthsciencessc.rpms2.process_engine.core.DefaultProcess))

(defn default-get-login
  "Redirect to view-login"
  [ctx]
  (redirect "/view/login"))

(defn hdr-greeting  
  "Return standard header."
  [pageheader]
  [:div [:div#home [:h1 pageheader]] "[PROGRESS DOTS GO HERE]" ])

(defn username
  []
  (-> (session-get :user) :username))

(defn footer
  []
  [:div.ui-grid-b
   [:div.ui-block-a (hpages/link-to "/login" "login" ) ]
   [:div.ui-block-b "RPMS2" ]
   [:div.ui-block-c (if-let [name (username)]
                      [:div#header-userid name
                       (if-let [loc (session-get :location)] 
                         (str " @ " loc))])]])

(defn rpms2-page
  "Emits a standard RPMS2 page."
  [content & {:keys [pageheader]}]
  (hpages/html5
   [:head
    (hpages/include-css 
     "/app.css"
     "/jquery.mobile-1.0.1.min.css")
    ;; TODO figure out if absolute paths work here
    ;; or use the path for the current page to construct
    ;; the relative paths
    (hpages/include-js "../../../../app.js"
                       "../../../../jquery-1.6.4.js"
                       "../../../../jquery.mobile-1.0.1.js")]
   [:body
    [:div#header {:data-role "header" } (hdr-greeting pageheader)
     (if-let [msg (flash-get :header)]
       [:div#flash msg])]
    [:div#content {:data-role "content" } content]
    [:div#footer {:data-role "footer" } (footer)]]))


(defn default-get-view-login
  "Returns HTML login form"
  [ctx]
  (rpms2-page (form-login) :pageheader (i18n :hdr-login)))

(defn default-post-view-login
  "Authenticates using username and password.  
   If successful, saves user in session and redirects to /view/select/location; 
   otherwise sets flash error message and redirects to /view/login.  "

  [{{:keys [userid password]} :body-params} ]
  (let [resp (dsa/authenticate userid password)]
    (if (= (:status resp) 200)
      (do
        (let [bb (:json resp) ]
        	(debug "USER RECORD => " bb )
        	(session-put! :user bb))
        (debug "LOGIN succeeded: " userid)
        (redirect "/view/select/location"))
      (do 
        (flash-put! :header (i18n :flash-invalid-login))
        (debug "LOGIN FAILED:" userid)
        (redirect "/view/login")))))

(defn default-get-view-select-consenter 
  [_]
  "STUB")

(defn authorized-locations
  [user]
  (->> user
       :role-mappings
       (filter (comp #{"Consent Collector"} :name :role))
       (map :location)))

(defn default-post-view-select-location
   "Save the location, then go to /view/select/lock-code"
   [{{:keys [location]} :body-params } ]

   (debug "Location has been selected: " location)
   (session-put! :location location)
   (redirect "/view/select/lock-code"))

(defn default-get-view-select-location
  "Based on the number of authorized locations for logged in user: 
     1. If no locations, redirects to view-not-authorized   
     2. If 1 location, that location and it's parent organization is set 
  	in session context and user is redirected to view-select-lock-code.
     3. If more than one location, displays the page to select the locations.

     Pivotal Tracker: https://www.pivotaltracker.com/story/show/26014553"
  [_]
  (try 
    (let [locs-data (authorized-locations (session-get :user))
          locs-names (map :name locs-data)]
      (debug "default-get-view-select-location -> locs = " locs-names)
      (if (empty? locs-names)
        (redirect "/view/not-authorized")
        (if (= (count locs-names) 1)  
          (let [l (first locs-data)]
            (debug "Using Location/Org information: | " l "|")
            (session-put! :org-location l)
            (redirect "/view/select/lock-code"))
          (rpms2-page (form-select-location locs-names)
                      :pageheader (i18n :hdr-select-location)))))
    (catch Exception ex (str "default-get-view-select-location Failed: " ex))))


(defn default-get-view-not-authorized 
  [_]
  (rpms2-page (i18n :not-authorized-message) :pageheader "NOT AUTHORIZED HEADER"))

(defn valid-lock-code?
	[lockcode]
  (and (string? lockcode)
       (re-matches #"\d{4}" lockcode)))

(defn default-post-view-select-lock-code
  "Validates lock code.  If valid, continue on to /view/select/consenter.
  Otherwise, go back to /view/select/lock-code to try again.
  Sets flash message if lock code is invalid."

  [ { {:keys [lockcode]} :body-params } ]
  (debug "default-post-view-select-lock-code: lockcode is "  lockcode)
  (if (valid-lock-code? lockcode) 
    (do
      (session-put! :lockcode lockcode)
      (redirect "/view/select/consenter"))
    (do
      (session-delete-key! :lockcode)
      (info "Invalid lockcode " lockcode)
      (flash-put! :header (i18n :flash-invalid-lockcode))
      (redirect "/view/select/lock-code"))))

(defn default-get-view-select-lock-code
  "Form to enter lock code. "
  [_]
  (rpms2-page (form-select-lock-code)
              :pageheader (i18n :hdr-select-lockcode)))


(def processes [{:name "get-login"
                 :runnable-fn (constantly true)
                 :run-fn default-get-login}

                {:name "get-view-login"
                 :runnable-fn (fn [context] (not (contains? context :password))),
                 :run-fn default-get-view-login}

                {:name "post-view-login"
                 :runnable-fn (constantly true)
                 :run-fn default-post-view-login}

                {:name "get-view-not-authorized"
                 :runnable-fn (constantly true)
                 :run-fn default-get-view-not-authorized}

                {:name "get-view-select-lock-code"
                 :runnable-fn (constantly true)
                 :run-fn default-get-view-select-lock-code}

                {:name "post-view-select-lock-code"
                 :runnable-fn (constantly true)
                 :run-fn default-post-view-select-lock-code}
                                
                {:name "post-view-select-location"
                 :runnable-fn (constantly true)
                 :run-fn default-post-view-select-location}
                                
                {:name "get-view-select-location"
                 :runnable-fn (constantly true)
                 :run-fn default-get-view-select-location}

                {:name "get-view-select-consenter"
                 :runnable-fn (constantly true)
                 :run-fn default-get-view-select-consenter}
 		])

(pe/register-processes (map #(DefaultProcess/create %) processes))
(debug "Processes have been registered")

(defn debug-ring
  [f]
  (fn [arg]
    (debug "RING BEFORE:")
    (pprint (select-keys arg [:uri :body :status :request-method]))
    (try
      (let [ret (f arg)]
        (debug "RING AFTER:")
        (pprint ret)
        ret)
      (catch Exception e
        (debug "RING ERROR: " (.getMessage e))
        (.printStackTrace e)
        (throw e)))))

;; Enable session handling via sandbar 
;; Make resources/public items in search path
(def app (-> (ws/ws-constructor)
             (sandbar.stateful-session/wrap-stateful-session)
             (debug-ring)
             (ring.middleware.resource/wrap-resource "public")))
