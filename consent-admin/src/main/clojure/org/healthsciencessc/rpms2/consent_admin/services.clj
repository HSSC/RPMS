(ns org.healthsciencessc.rpms2.consent-admin.services
  (:require [clj-http.client :as client]
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
  [user]
  {:basic-auth [(:username user) (:password user)]})

(defn- defaults
  "Defines any defaults that will be placed on all requests."
  []
  {:as :clojure :throw-exceptions false})

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
  [user url params & handlers]
  (DO client/get 
      (full-url url params) 
      (merge (credentials user) (defaults)) 
      handlers))

(defn- POST
  "Makes a post request to the server"
  [user url params form body & handlers]
  (DO client/post 
      (full-url url params) 
      (merge {:body body :form-params form} (credentials user) (defaults))
      handlers))

(defn- PUT
  "Makes a put request to the server"
  [user url params form body & handlers]
  (DO client/put 
      (full-url url params) 
      (merge {:body body :form-params form} (credentials user) (defaults))
      handlers))

(defn- DELETE
  "Makes a delete request to the server"
  [user url params form body & handlers]
  (DO client/delete 
      (full-url url params) 
      (merge {:body body :form-params form} (credentials user) (defaults))
      handlers))

;; Define Public Specific Integration Functions.

(defn authenticate
  "Calls the authentication process within the consent services."
  [username password]
  (GET {:username username :password password} "/security/authenticate" {} 
       (fn [r] (if (= 200 (:status r)) (assoc (:body r) :password password )))))