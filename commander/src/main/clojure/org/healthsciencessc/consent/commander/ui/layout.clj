(ns org.healthsciencessc.consent.commander.ui.layout
  (:require [org.healthsciencessc.consent.commander.ui.navigation :as nav]
            [pliant.webpoint.common :as common]
            [pliant.webpoint.response :as response]
            [hiccup.page :as page]
            [hiccup.core :as hcup]
            [sandbar.stateful-session :as sess]
            [clojure.data.json :as json])
  (:use     [pliant.process :only [defprocess deflayer continue do-before]]))


(defprocess scripts
  "Generates a list of URLs that are injected into the head section of the layout as script files(javascript)."
  [ctx]
  ["/js/jquery-1.7.2.min.js"
   "/js/jquery-ui-1.8.19.custom.min.js"
   "/js/utils.js"
   "/js/dialog.js"
   "/js/pane.js"
   "/js/consent-admin.js"
   "/js/consent-designer.js"
   "/js/consent-designer-ext.js"])


(defprocess stylesheets
  "Generates a list of URLs that are injected into the head section of the layout as stylesheets."
  [ctx]
  ["/css/clean.css"
   "/css/redmond/jquery-ui-1.8.19.custom.css"
   "/css/dialog.css"
   "/css/consent-admin.css"
   "/css/consent-designer.css"])
  
(defn- header
  "Creates the default header that is used for the application"
  []  
    [:div#header.header
      [:h3#headertitle "Consent Management - Administration"]
      [:ul#loginstat.headerlist
        [:li#current-user.first [:span {:onclick "PaneManager.stack(\"/view/profile\", {}, {})"} (get (sess/session-get :user) :username)]]
        [:li#logout [:span {:onclick "PaneManager.logout();"} "Logout"]]]])

(defn- header-no-session
  "Creates the default header that is used for the application"
  []  
    [:div#header.header])
 
(defn- footer
  "Creates the default header that is used for the application"
  [ctx]
  [:div#footer.footer 
    [:span#footerbrand "Research Permissions Management System"]
    [:span#footerorg [:a {:href "http://www.healthsciencessc.org" :target "_blank"} "Health Sciences of South Carolina" ]]
    [:span#footerversion "Version 2.0.0-SNAPSHOT"]])

(defn- leftbar
  "Creates the default header that is used for the application"
  []
  [:div#leftbar.leftbar (nav/navigator)])

(defn- body
  "Creates the default layout that is used for the application"
  [ctx elements]
  [:body 
    (if (not-any? #(= (common/path ctx) %) ["/view/home" "/security/login" "/login"])
      [:script (str "PaneManager.triggerOnInit(\"" (:path-info ctx) "\", {},{});")])
    [:div#page.page
               (header)
               (leftbar)
               [:div#content.content elements]
               (footer ctx)]])

(defn- body-no-session
  "Creates the default layout that is used for the application"
  [ctx elements]
  [:body [:div#page.page
               (header-no-session)
               [:div#content.content elements]
               (footer ctx)]])

(defn- head
  "Creates the head section of the page."
  [ctx]
  [:head [:meta {:charset "utf-8"}]
         [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
         [:title "RPMS Administration"]
    (apply page/include-css (stylesheets ctx))
    (apply page/include-js (scripts ctx))
    [:script (str "Utils.Url.initBasePath(\"" (:context ctx) "\");")]])

(defn- layout 
  "Generates the standard layout when in a validated session."
  [ctx elements]
  (page/html5
    (head ctx)
    (body ctx elements)))

(defn- layout-no-session
  "Generates the standard layout when not in a validated session."
  [ctx elements]
  (page/html5
    (head ctx)
    (body-no-session ctx elements)))

(defn- pane
  "Creates a structure representing a pane.  Accepts a request context, title, and pane content and returns the appropriate structure."
  [ctx title elements]
  (hcup/html 
    [:div.content-title.ui-helper-reset.ui-state-default.ui-corner-all (or title "No Title")]
    [:div.content-data elements]))

(defprocess render
  "Decides how markup should be wrapped in a container.  This provides 
   the ability to add additional containers later based on how the 
   request was made.  IE - make into a portlet."
  [request title & elements]
  (response/respond-with-html (layout request elements) request))

(deflayer render render-no-session
  "Renders into a layout specific for responses that are outside of an authenticated session."
  [request title & elements]
  (if (not (sess/session-get :user))
    (response/respond-with-html (layout-no-session request elements) request)
    (continue)))

(deflayer render render-pane
  "Renders into a layout specific for a pane that is dynamically injected into the DOM on the client side."
  [request title & elements]
  (if (= (get-in request [:query-params :view-mode]) "pane")
    (response/respond-with-html (pane request title elements) request)
    (continue)))

(do-before render render-no-session render-pane)

(defn- error-html
  [error]
  (or (:message error)
      "An Error Happened.  Darnit."))

(defn- pane-error
  [error]
  {:status (or (:status error) 500)
   :headers (or (:headers error) {"Content-Type" "application/json"})
   :body (json/json-str (or (:body error) {:message (or (:message error) "Requested process failed to complete.")}))})           

(defn render-error
  [ctx error]
  (cond
    (not (sess/session-get :user))
      (layout-no-session (error-html error))
    (= (get-in ctx [:query-params :view-mode]) "pane")
      (pane-error error)
    :else
      (layout (error-html error))))
