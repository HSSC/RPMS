(ns org.healthsciencessc.rpms2.consent-admin.ui.layout
  (require [org.healthsciencessc.rpms2.process-engine.path :as path]
           [hiccup.page :as page]
          [hiccup.element :as element]
           [sandbar.stateful-session :as sess])
  (use [org.healthsciencessc.rpms2.consent-admin.ui.burp]))

(defn header
  "Creates the default header that is used for the application"
  [params & options]  
    [:div#header.header
      [:h1 "RPMS"]
      [:div#loginstat
        [:h4#username (get (sess/session-get :user) :username)]
        (element/link-to "/security/logout"
                         [:h4#logout "Logout"])]])

(defn header-no-session
  "Creates the default header that is used for the application"
  [params & options]  
    [:div#header.header
      [:h1 "RPMS"]])
 
(defn footer
  "Creates the default header that is used for the application"
  [params & options]
  [:div#footer.footer [:h3 "Page Footer"]])

(defn leftbar
  "Creates the default header that is used for the application"
  [params & options]
  [:div#leftbar.leftbar "I'm the leftbar"])

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
    (page/include-css "/css/clean.css"
                      "/css/consent-admin.css"
                      "/css/redmond/jquery-ui-1.8.19.custom.css")])

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

