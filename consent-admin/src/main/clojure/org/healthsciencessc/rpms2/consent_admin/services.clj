(ns org.healthsciencessc.rpms2.consent-admin.services
  (:require [clj-http.client :as client]
            [sandbar.stateful-session :as sess]
            [org.healthsciencessc.rpms2.consent-domain.types :as domain]
            [clojure.pprint :as pp]
            [clojure.stacktrace :as st]
            [hiccup.util :as hutil])
  (:use [org.healthsciencessc.rpms2.consent-admin.config]
        [clojure.data.json :only (json-str read-json)]))

;; Request And Response Support

(defn service-error?
  "Any response that isn't HTTP 200 from consent-services will
  assoc the response as metadata."
  [m]
  (not (nil? (meta m))))

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

(defn- meta-failure-handler
  "Simple handler that attaches error and response information as metadata to a returned map if request is unsuccessful."
  [coll message]
  (fn [resp]
    (if (not= (:status resp) 200)
      (let [m (assoc resp :message message)]
        (with-meta coll m)))))

(def ^:private failure-handler
  (meta-failure-handler {} "Something went wrong..."))

(defn- handle-response
  "Handle the response from all requests.
   xs is a list of handlers to casacde through."
  [resp xs]
  (let [handlers (concat xs [failure-handler #(:body %)])]
    (first (drop-while (complement identity)
                (map #(% resp)
                     handlers)))))

;; The HTTP Request Functions

(defn- DO
  "Performs the actual http request, applying any handlers that are needed."
  [method url settings handlers]
  (try
    (handle-response (method url settings) handlers)
    (catch Exception e
      ;; Handle Some What The Fudge Situations
      (st/print-stack-trace e)
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
                 :invalid))]))

;; Domain utilities

;; LOCATIONS
(defn get-locations
  [_]
  (GET "/security/locations" {}))

(defn get-location
  [id]
  (GET "/security/location" {:location id}))

(defn add-location
  [l]
  (PUT "/security/location"
        nil
        nil
        (with-out-str (prn l))))

(defn edit-location
  [id l]
  (POST "/security/location"
        {:location id}
        nil
        (with-out-str (prn l))))

;; USERS
(defn get-users
  [_]
  (GET "/security/users" {}))

(defn get-user
  [id]
  (GET "/security/user" {:user id}))

(defn add-user
  [u]
  (PUT "/security/user"
        nil
        nil
        (with-out-str (prn u))))


(defn edit-user
  [id u]
  (POST "/security/user"
        {:user id}
        nil
        (with-out-str (prn u))))

;; ORGANIZATIONS
(defn delete-organization
  [id]
  (DELETE "/security/organization" {:organization id} nil nil))

(defn get-organizations
  [_]
  (GET "/security/organizations" {}))

(defn add-organization
  [o]
  (PUT "/security/organization"
       nil
       nil
       (with-out-str (prn o))))

(defn edit-organization
  [id o]
  (POST "/security/organization"
        {:organization id}
        nil
        (with-out-str (prn o))))

(defn get-organization
  [id]
  (GET "/security/organization" {:organization id}))


;; ROLES
(defn get-roles
  []
  (GET "/security/roles" {}))

(defn get-role
  [id]
  (GET "/security/role" {:role id}))

(defn add-role
  [r]
  (PUT "/security/role"
        nil
        nil
        (with-out-str (prn r))))

(defn edit-role
  [id r]
  (POST "/security/role"
        {:role id}
        nil
        (with-out-str (prn r))))

;; GROUPS
(defn get-groups
  [_]
  (GET "/security/groups" {}))

(defn get-group
  [id]
  (GET "/security/group" {:group id}))

(defn add-group
  [g]
  (PUT "/security/group"
        nil
        nil
        (with-out-str (prn g))))

(defn edit-group
  [id g]
  (POST "/security/group"
        {:group id}
        nil
        (with-out-str (prn g))))

(defn add-role-to-user
  [u r])

(defn add-role-to-group
  [g r])

(defn add-admin
  [u]
  (let [org-id (:id (:organization u))
        admin-id (-> (filter #(= (:code %)
                             domain/code-role-admin)
                             (get-roles))
                   first
                   :id)
        usr-resp (add-user u)]
    (if (:id usr-resp)
      (let [role-params {:user (:id usr-resp)
                         :role admin-id}]
            (PUT "/security/userrole"
               role-params
               nil
               nil))
      usr-resp)))  ;; pass this back directly

