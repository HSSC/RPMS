(ns org.healthsciencessc.rpms2.consent-admin.ui.layout
  (:require [org.healthsciencessc.rpms2.process-engine.path :as path]
           [org.healthsciencessc.rpms2.consent-admin.ui.navigation :as nav]
           [hiccup.page :as page]
           [hiccup.core :as hcup]
           [sandbar.stateful-session :as sess]))

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
    [:span#footerversion "Version 2.0.0-SNAPSHOT"]
    [:div#dialog  "Are you sure you want to end this session?"]])

(defn- leftbar
  "Creates the default header that is used for the application"
  []
  [:div#leftbar.leftbar (nav/navigator)])

(defn- body
  "Creates the default layout that is used for the application"
  [ctx elements]
  [:body 
    (if (not= (:path-info ctx) "/view/home")
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
    (page/include-css "/css/clean.css"
                      "/css/redmond/jquery-ui-1.8.19.custom.css"
                      "/css/consent-admin.css")
    (page/include-js "/js/jquery-1.7.2.min.js"
                     "/js/jquery-ui-1.8.19.custom.min.js"
                     "/js/pane.js"
                     "/js/consent-admin.js")
    [:script (str "PaneManager.initBasePath(\"" (:context ctx) "\");")]])

(defn- layout 
  ""
  [ctx elements]
  (page/html5
    (head ctx)
    (body ctx elements)))

(defn- layout-no-session
  ""
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

(defn render
  "Decides how markup should be wrapped in a container.  This provides 
   the ability to add additional containers later based on how the 
   request was made.  IE - make into a portlet."
  [ctx title & elements]
  (cond
    (not (sess/session-get :user))
      (layout-no-session ctx elements)
    (= (get-in ctx [:query-params :view-mode]) "pane")
      (pane ctx title elements)
    :else
      (layout ctx elements)))
