(ns org.healthsciencessc.rpms2.consent-admin.services
  (:require [clj-http.client :as client]
            [sandbar.stateful-session :as sess]
            [clojure.pprint :as pp]
            [hiccup.util :as hutil])
  (:use [org.healthsciencessc.rpms2.consent-admin.config]
        [clojure.data.json :only (json-str read-json)]))

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

(defn- failure-handler
  "Simple handler that returns the body if the status is 200"
  [resp]
  (if (not= (:status resp) 200)
    resp))


(defn- meta-failure-handler
  "Simple handler that attaches error and response information as metadata to a returned map if request is unsuccessful."
  [message]
  (fn [resp]
    (if (not= (:status resp) 200)
      (with-meta {} {:response resp :message message :body (:body resp)}))))

(defn- handle-response
  "Handle the response from all requests."
  [resp handlers]
  (or (first (drop-while (complement identity) (map #(% resp) handlers)))
      (failure-handler resp)
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

(defn get-users
  [_]
  (GET "/security/users" {}))
(comment
(defn add-user
  [org-id o]
  (PUT "/security/user"
        nil
        nil
        (with-out-str (prn o))
        (fn [r] (if-not (= 200 (:status r))
                        {:invalid true}))))

(defn edit-user
  [id o]
  (POST "/security/user"
        {:user id} 
        nil
        (with-out-str (prn o))
        (fn [r] (if-not (= 200 (:status r))
                        {:valid false}
                        {:valid true}))))
)
(defn get-organizations
  [_]
  (GET "/security/organizations" {}))

(defn add-organization
  [o]
  (PUT "/security/organization"
       nil
       nil
       (with-out-str (prn o))
       (fn [r]
         (if (= 200 (:status r))
           {:valid true}
           {:valid false}))))

(defn edit-organization
  [id o]
  (POST "/security/organization"
        {:organization id} 
        nil
        (with-out-str (prn o))
        (fn [r]
          (if (= 200 (:status r))
            {:valid true}
            {:valid false}))))
 
(defn get-organization
  [id]
  (GET "/security/organization" {:organization id}))
