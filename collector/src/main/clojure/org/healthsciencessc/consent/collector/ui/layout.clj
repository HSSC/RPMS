(ns org.healthsciencessc.consent.collector.ui.layout
  (:require [org.healthsciencessc.consent.client.whoami :as whoami]
            [org.healthsciencessc.consent.collector.common :as common]
            [org.healthsciencessc.consent.collector.state :as state]
            [org.healthsciencessc.consent.collector.text :as text]
            [org.healthsciencessc.consent.collector.version :as version]
            [org.healthsciencessc.consent.collector.ui.action :as action]
            [org.healthsciencessc.consent.collector.ui.content :as uicontent]
            
            [hiccup.page :as page]
            [hiccup.core :as hcup]
            
            [sandbar.stateful-session :as sess]
            [clojure.data.json :as json]
            [pliant.webpoint.response :as response])
  
  (:use     [pliant.process :only [defprocess deflayer continue]]))

(defprocess scripts
  "Generates a list of URLs that are injected into the head section of the layout as script files(javascript)."
  [ctx options]
  ["/js/jquery-1.9.1.js"
   "/js/utils.js"
   "/js/dialog.js"
   "/js/collect.js"
   "/js/request.js"
   "/js/form.js"
   "/js/action.js"
   "/js/ui.js"
   "/js/jquery.mobile-1.3.0.js"])

(defprocess stylesheets
  "Generates a list of URLs that are injected into the head section of the layout as stylesheets."
  [ctx options]
  ["/css/jquery.mobile-1.3.0.css"
   "/css/content.css"
   "/css/form.css"
   "/css/dialog.css"])
  
(defprocess header
  "Creates the default header that is used for the application when in an authenticated session."
  [ctx options]  
  [:div.ui-grid-b.header {:data-role "header" :data-position "fixed" :data-theme "a"}
    [:div.ui-block-a.left-block 
      (cond 
        (:header-left options) (:header-left options)
        (state/in-session?) (action/cancel {:inline true}))]
    [:div.ui-block-b.center-block
      (if (:header-center options) 
        (:header-center options) 
        (uicontent/block-text (:title options)))]
		[:div.ui-block-c.right-block 
      (if (:header-right options) 
        (:header-right options) 
        (action/logout {:inline true}))]])

(defprocess header-no-session
  "Creates the default header that is used for the application when not in an authenticated session."
  [ctx options]  
  [:div.ui-grid-b.header {:data-role "header" :data-position "fixed" :data-theme "a"}
   	[:div.ui-block-a.left-block 
      (if (:header-left options) 
        (:header-left options))]
    [:div.ui-block-b.center-block
      (if (:header-center options) 
        (:header-center options) 
        (uicontent/block-text (:title options)))]
    [:div.ui-block-c.right-block 
      (if (:header-right options) 
        (:header-right options))]])
 
(defprocess footer
  "Creates the default footer that is used for the application when in an authenticated session."
  [ctx options]
  [:div.ui-grid-b.footer {:data-role "footer" :data-theme "c" :data-position "fixed"}
		[:div.ui-block-a.left-block 
      (cond 
        (:footer-left options) (:footer-left options)
        (state/get-encounter)
          (uicontent/block-text-small 
            (text/consenter-text :footer.consenter.label {:args [(common/formal-name (state/get-consenter))]})
            (text/consenter-text :footer.consenter.id.label {:args [(:consenter-id (state/get-consenter))]})
            (text/encounter-text :footer.encounter.label {:args [(:encounter-id (state/get-encounter))]}))
        (state/get-consenter)
          (uicontent/block-text-small 
            (text/consenter-text :footer.consenter.label {:args [(common/formal-name (state/get-consenter))]})
            (text/consenter-text :footer.consenter.id.label {:args [(:consenter-id (state/get-consenter))]})))]
		[:div.ui-block-b.center-block 
      (cond 
        (:footer-center options) (:footer-center options)
        (:name (state/get-location))
          (uicontent/block-text 
            (text/format-text :footer.organization.label {:args [(:name (state/get-organization))]})
            (text/location-text :footer.location.label {:args [(:name (state/get-location))]}))
        :else
          (uicontent/block-text 
            (text/format-text :footer.organization.label {:args [(:name (state/get-organization))]})))]
		[:div.ui-block-c.right-block 
      (if (:footer-right options) 
        (:footer-right options)
        (version/version-label))]])

(defprocess footer-no-session
  "Creates the default footer that is used for the application when not in an authenticated session."
  [ctx options]
  [:div.ui-grid-b.footer {:data-role "footer" :data-theme "c" :data-position "fixed"}
		[:div.ui-block-a.left-block 
      (if (:footer-left options) 
        (:footer-left options))]
    [:div.ui-block-b.center-block 
      (if (:footer-center options) 
        (:footer-center options))]
    [:div.ui-block-c.right-block 
      (if (:footer-right options) 
        (:footer-right options)
        (version/version-label))]])

(defprocess content
  "Creates the default content block that is used for the application when in an authenticated session."
  [ctx options elements]
  (if (:uigenerator options)
    [:div.content.clientuigenerator 
     (merge (:uigenerator-data options) 
            {:data-role "content" :data-theme "d" :data-uigenerator (:uigenerator options)}) elements]
    [:div.content {:data-role "content" :data-theme "d"} elements]))

(defprocess content-no-session
  "Creates the default content block that is used for the application when in an authenticated session."
  [ctx options elements]
  [:div.content {:data-role "content" :data-theme "d"} elements])

(defn page-div
  [options]
  (keyword (str "div#" (or (:pageid options) (System/currentTimeMillis)) ".page")))

(defprocess page
  "Creates the default page layout that is used for the application when not in an authenticated session."
  [ctx options elements]
  [(page-div options)
     {:data-role "page" :data-theme "d"}
   (header ctx options)
   (content ctx options elements)
   (footer ctx options)])

(defprocess page-no-session
  "Creates the default page layout that is used for the application when not in an authenticated session."
  [ctx options elements]
  [(page-div options)
     {:data-role "page" :data-theme "d"}
   (header-no-session ctx options)
   (content-no-session ctx options elements)
   (footer-no-session ctx options)])

(defprocess body
  "Creates the default layout that is used for the application when in an authenticated session."
  [ctx options elements]
  [:body (page ctx options elements)])

(defprocess body-no-session
  "Creates the default layout that is used for the application when not in an authenticated session."
  [ctx options elements]
  [:body (page-no-session ctx options elements)])

(defprocess head
  "Creates the head section of the document when in an authenticated session."
  [ctx options]
  [:head [:title (:title options)]
         [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
         
    (if (nil? (get-in ctx [:query-params :pageRequest]))
      (list
        (apply page/include-css (stylesheets ctx options))
        (apply page/include-js (scripts ctx options))
        [:script (str "Utils.Url.initBasePath(\"" (:context ctx) "\");")]))])

(defprocess head-no-session
  "Creates the head section of the document when not in an authenticated session."
  [ctx options]
  [:head [:title (:title options)]
         [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    (if (nil? (get-in ctx [:query-params :pageRequest]))
      (list
        (apply page/include-css (stylesheets ctx options))
        (apply page/include-js (scripts ctx options))
        [:script (str "Utils.Url.initBasePath(\"" (:context ctx) "\");")]))])

(defprocess page-layout
  "Generates the standard layout when in an authenticated session."
  [ctx options elements]
  (page/html5
    (head ctx options)
    (body ctx options elements)))

(defprocess page-layout-no-session
  "Generates the standard layout when not in an authenticated session."
  [ctx options elements]
  (page/html5
    (head-no-session ctx options)
    (body-no-session ctx options elements)))

(defprocess render-page
  "Decides how markup should be wrapped in a container.  This provides 
   the ability to add additional containers later based on how the 
   request was made.  IE - make into a portlet."
  [request options & elements]
  (response/respond-with-html (page-layout request options elements) request))

(deflayer render-page render-page-no-session
  "Renders into a layout specific for responses that are outside of an authenticated session."
  [request options & elements]
  (if (not (whoami/identified?))
    (response/respond-with-html (page-layout-no-session request options elements) request)
    (continue)))

(defn dialog-div
  [options]
  (keyword (str "div#" (or (:dialogid options) (System/currentTimeMillis)) ".dialog")))

(defprocess dialog
  "Creates the default page layout that is used for the application when not in an authenticated session."
  [request options elements]
  [(dialog-div options)
     {:data-role "dialog" :data-theme "a"}
     [:div.header {:data-role "header"} 
      [:h1 {:data-role "heading"} (:title options)]]
     (content request options elements)])

(defprocess render-dialog
  "Decides how markup should be wrapped in a dialog container.  This provides 
   the ability to add additional containers later based on how the 
   request was made.  IE - make into a portlet."
  [request options & elements]
  (response/respond-with-html 
    (page/html5
      [:head [:title (:title options)]
           [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]]
      [:body (dialog request options elements)]) request))

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
  [request error]
  (cond
    (not (sess/session-get :user))
      (page-layout-no-session (error-html error))
    (= (get-in request [:query-params :view-mode]) "pane")
      (pane-error error)
    :else
      (page-layout (error-html error))))
