(ns org.healthsciencessc.consent.collector.respond
  (:require [org.healthsciencessc.consent.collector.ui.layout :as layout]
            [ring.util.response :as ring]
            [pliant.webpoint.response :as response]
            [pliant.webpoint.url :as url]))

;; Success(200) Responses
(defn with-xml
  [xml] (ring/content-type (ring/response xml) "text/xml"))

(defn with-actions
  [data & actions]
  (merge data {:actions actions}))

(defn add-view
  [url]
  {:action "changeView" :view-url url :reset false})

(defn reset-view
  [url]
  {:action "changeView" :view-url url :reset true})


;; Redirect(300) Responses
(defn redirect
  [request url]
  (ring/redirect (url/root-link request url)))

;; Error(400) Responses
(defn forbidden-api
  ([request] (forbidden-api request "You do not have the authority to execute the requested process."))
  ([request message]
    (ring/status (response/respond-with-data {:message message} request) 403)))


(defn forbidden-view
  ([request] (forbidden-view request "You do not have the authority to execute the requested view."))
  ([request message]
    (ring/status (layout/render-error request message) 403)))


(defn with-error
  ([request] (with-error request "Unable to process the request"))
  ([request message]
    (ring/status (response/respond-with-data {:message message} request) 400)))

