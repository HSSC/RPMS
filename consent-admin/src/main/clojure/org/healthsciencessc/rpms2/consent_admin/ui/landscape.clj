(ns org.healthsciencessc.rpms2.consent-admin.ui.landscape
  (require [org.healthsciencessc.rpms2.process-engine.path :as path]
           [hiccup.page :as page])
  (use [org.healthsciencessc.rpms2.consent-admin.ui.burp]))

(defn content
  "Creates a content panel."
  [params options]
  [:div (:content options)])

(defn header
  "Creates the default header that is used for the application"
  [params options]  
  [:div#header [:h1 "RPMS"]])

(defn footer
  "Creates the default header that is used for the application"
  [params options]
  [:div#footer [:h3 "Page Footer"]])

(defn header-no-session
  "Creates the header that is used for the application when a session is active."
  [params options]  
  [:div#header [:h1 "RPMS"]])

(defn footer-no-session
  "Creates the header that is used for the application when a session is not active."
  [params options]
  [:div#footer [:h3 "Page Footer"]])

(defn leftbar
  "Creates the default header that is used for the application"
  [params options]
  [:div "I'm the leftbar"])

(defn body
  "Creates the default layout that is used for the application"
  [params options]
  [:body [:div#page.page (header params options) 
                         (leftbar params options) 
                         (content params options)
                         (footer params options)]])

(defn body-no-session
  "Creates the default layout that is used for the application"
  [params options]
  [:body [:div (header-no-session params options) 
               (content params options) 
               (footer-no-session params options)]])

(defn head
  "Creates the head section of the page."
  [params options]
  [:head [:meta {:charset "utf-8"}]
         [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
         [:link {:rel "stylesheet" :type "type/css" :href (path/root-link params "/css/clean.css")}]
         [:link {:rel "stylesheet" :type "type/css" :href (path/root-link params "/css/consent-admin.css")}]
         [:link {:rel "stylesheet" :type "type/css" :href (path/root-link params "/css/redmond/jquery-ui-1.8.19.custom.css")}]
         [:script {:src (path/root-link params "/js/consent-admin.js")}]
         [:script {:src (path/root-link params "/js/jquery-1.7.2.min.js")}]
         [:script {:src (path/root-link params "/js/jquery-ui-1.8.19.custom.min.js")}]])


