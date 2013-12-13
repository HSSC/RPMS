(ns org.healthsciencessc.consent.collector.respond
  (:require [ring.util.response :as ring]
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
  [ctx url]
  (ring/redirect (url/root-link ctx url)))

;; Error(400) Responses
(defn not-found
  ([] (not-found "The requested resource was not found."))
  ([message] (ring/not-found {:message message})))

(defn forbidden
  ([] (forbidden "You do not have the authority to execute the requested process."))
  ([message]
    (ring/status (ring/response {:message message}) 403)))


(defn forbidden-view
  ([] (forbidden "You do not have the authority to execute the requested view."))
  ([message]
    (ring/status (ring/response {:message message}) 403)))


(defn with-error
  ([] (with-error "Unable to process the request"))
  ([message]
    (ring/status (ring/response {:message message}) 400)))

