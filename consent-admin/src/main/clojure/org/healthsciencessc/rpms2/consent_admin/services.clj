(ns org.healthsciencessc.rpms2.consent-admin.services
  (:require [clj-http.client :as client]
            [sandbar.stateful-session :as sess]
            [hiccup.util :as hutil])
  (:use [org.healthsciencessc.rpms2.consent-admin.config]
        [clojure.data.json :only (read-json)]))

;; Request And Response Support

(defn- full-url
  "Creates the absolute URL to the services using the configured path to services."
  [url params]
  (.toString (hutil/url (:services.url config) url params)))

(defn- credentials
  "Creats a map of all the header items needed for basic authentication."
  ([] (credentials
        (select-keys (sess/session-get :user)
                     [:username :password])))
  ([user] {:basic-auth [(:username user) (:password user)]}))

(defn- defaults
  "Defines any defaults that will be placed on all requests."
  []
  {:as :clojure ;; this tells clj-http to read-string on the body
   :content-type "application/clojure" ;; tells our services to serve the Right Stuff
   :throw-exceptions false})

(defn- handle-response
  "Handle the response from all requests."
  [resp handlers]
  (or (first (drop-while (complement identity) (map #(% resp) handlers)))
      (:body resp)))

;; The HTTP Request Functions

(defn- DO
  "Performs the actual http request, applying any handlers that are needed."
  [method url settings handlers]
  (try 
    (handle-response (method url settings) handlers)
    (catch Exception e
      ;; Handle Some What The Fudge Situations
      (.printStackTrace e)
      nil)))

(defn- GET
  "Makes a get request to the server"
  [url params & handlers]
  (DO client/get 
      (full-url url params) 
      (merge (credentials) (defaults)) 
      handlers))

(defn- POST
  "Makes a post request to the server"
  [url params form body & handlers]
  (DO client/post 
      (full-url url params) 
      (merge {:body body :form-params form} (credentials) (defaults))
      handlers))

(defn- PUT
  "Makes a put request to the server"
  [url params form body & handlers]
  (DO client/put 
      (full-url url params) 
      (merge {:body body :form-params form} (credentials) (defaults))
      handlers))

(defn- DELETE
  "Makes a delete request to the server"
  [url params form body & handlers]
  (DO client/delete 
      (full-url url params) 
      (merge {:body body :form-params form} (credentials) (defaults))
      handlers))

;; Define Public Specific Integration Functions.

(defn authenticate
  "Calls the authentication process within the consent services."
  [username password]
  (DO client/get 
        (full-url "/security/authenticate" {})
        (merge (credentials {:username username :password password}) (defaults)) 
        [(fn [r] (if (= 200 (:status r))
                 (assoc (:body r)
                        :password password)
                 {:invalid-auth true}))]))

;; Domain utilities
(defn get-locations
  [_]
  (GET "/security/locations" {}))

(defn get-organizations
  [_]
  (GET "/security/organizations" {}))

(defn get-users
  [_]
  (GET "/security/users" {}))
