(ns org.healthsciencessc.rpms2.consent-admin.ui.layout
  (require [org.healthsciencessc.rpms2.process-engine.path :as path]
           [hiccup.page :as page]
           [hiccup.element :as element]
           [sandbar.stateful-session :as sess]))

(defn header
  "Creates the default header that is used for the application"
  [params & options]  
    [:div#header.header
      [:h3#headertitle "Consent Management - Administration"]
      [:ul#loginstat.headerlist
        [:li#current-user.first [:span {:onclick "RPMS.launchInPane(\"/view/profile\")"} (get (sess/session-get :user) :username)]]
        [:li#logout [:span {:onclick "RPMS.logout();"} "Logout"]]]])

(defn header-no-session
  "Creates the default header that is used for the application"
  [params & options]  
    [:div#header.header])
 
(defn footer
  "Creates the default header that is used for the application"
  [params & options]
  [:div#footer.footer ])

(defn create-nav-item
  "Creates a list item for use in navigation"
  [url label]
  [:li.navitem [:a {:href "#" :onclick (str "RPMS.launchInPane(\"" url "\")") } label]])


(defn leftbar
  "Creates the default header that is used for the application"
  [params & options]
  [:div#leftbar.leftbar 
    [:div#navigator 
      (if true 
        (list [:h4#orgnavlabel [:a {:href "#"} "Organization"]] 
          [:div#orgnavpanel.navpanel
            [:ul#orgnavlist.navlist
              (create-nav-item "/view/organization" "Settings")
              (create-nav-item "/view/locations" "Locations")]]))
      (if true 
        (list [:h4#secnavlabel [:a {:href "#"} "Security"]] 
          [:div#secnavpanel.navpanel
            [:ul#secnavlist.navlist
              (create-nav-item "/view/users" "Users")
              (create-nav-item "/view/groups" "Groups")
              (create-nav-item "/view/roles" "Roles")]]) )
      (if true 
        (list [:h4#pronavlabel [:a {:href "#"} "Protocols"]] 
          [:div#pronavpanel.navpanel
            [:ul#pronavlist.navlist
              (create-nav-item "/view/protocol/new" "Create")
              (create-nav-item "/view/protocol/locations" "Locations")]]) )
      (if true 
        (list [:h4#libnavlabel [:a {:href "#"} "Library"]] 
           [:div#libnavpanel.navpanel
            [:ul#libnavlist.navlist
              (create-nav-item "/view/policy/definitions" "Policy Definitions")
              (create-nav-item "/view/policies" "Policies")
              (create-nav-item "/view/metaitems" "Meta Items")
              (create-nav-item "/view/widgets" "Widgets")]]) )]
    [:script 
"$(function() {
  $( \"#navigator\" ).accordion({
    collapsible: true,
    fillSpace: true,
    autoHeight: false
  });
});"]])

(defn body
  "Creates the default layout that is used for the application"
  [params content & options]
  [:body [:div#page.page
               (header params options)
               (leftbar params options)
               [:div#content.content content]
               (footer params options)]])

(defn body-no-session
  "Creates the default layout that is used for the application"
  [params content & options]
  [:body [:div#page.page
               (header-no-session params options)
               [:div#content.content content]
               (footer params options)]])

(defn head
  "Creates the head section of the page."
  [params & options]
  [:head [:meta {:charset "utf-8"}]
         [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    (page/include-js "/js/consent-admin.js"
                     "/js/jquery-1.7.2.min.js"
                     "/js/consent-admin.js"
                     "/js/jquery-ui-1.8.19.custom.min.js")
    (page/include-css "/css/consent-admin.css"
                      "/css/clean.css"
                      "/css/redmond/jquery-ui-1.8.19.custom.css")
    [:script (str "RPMS.setContext(\"" (:context params) "\");")]])

(defn layout 
  ""
  [ctx content]
  (page/html5
    (head ctx)
    (body ctx content)))

(defn layout-no-session
  ""
  [ctx content]
  (page/html5
    (head ctx)
    (body-no-session ctx content)))

